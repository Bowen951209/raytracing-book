#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;

layout (local_size_x = 16, local_size_y = 16) in;

const float INFINITY = 3.402823E+38;
const int MATERIAL_LAMBERTIAN = 0;
const int MATERIAL_METAL = 1;
const int MATERIAL_DIELECTRIC = 2;

uniform int sample_per_pixel; // Count random.glsl samples for each pixel.
uniform int max_depth;        // Maximum number of ray bounces into scene.
uniform float last_color_scale; // The color scale last time dispatch call applied.
uniform float this_color_scale; // The color scale this time dispatch call applies.
uniform float u_rand_factor; // The initial random vector. This is for varying randomness from call to call.

layout(std140, binding = 0) uniform Camera {
    float viewport_width;
    float viewport_height;
    float aspect_ratio;
    float defocus_angle;
    vec3 camera_pos;
    vec3 up_left_pos;
    vec3 pixel_delta_u;
    vec3 pixel_delta_v;
    vec3 defocus_disk_u;
    vec3 defocus_disk_v;
};

vec2 image_size;
vec2 pixel_coord;
float rand_factor = u_rand_factor;
bool should_scatter;
vec3 albedo;
float material;

struct Ray {
    vec3 o;     // origin
    vec3 dir;   // direction
    float time; // a factor that is in range [0, 1).
};

struct HitRecord {
    bool hit;
    bool is_front_face;
    vec3 p;
    vec3 normal;
    float t;
};

struct Sphere {
    vec3 center1;
    vec3 center_vec;
    float radius;
    vec3 albedo;
    float material;
};

struct Interval {
    float min;
    float max;
};

struct AABB {
    Interval x;
    Interval y;
    Interval z;
};

struct BVHNode {
    AABB bbox;
    float left_idx;
    float right_idx;
};

layout(std430, binding = 0) buffer ModelsBuffer {
    float spheres_count; // Count of spheres sent in from Java side.
    Sphere spheres[];
};

layout(std430, binding = 1) buffer BVHBuffer {
    BVHNode bvh_nodes[];
};

// The includes. Must be after the global variables and ssbos because some of the includes use those.
#include <utils/interval.glsl>
#include <utils/random.glsl>
#include <utils/hitting.glsl>
#include <utils/scatter.glsl>

bool interval_surrounds(Interval interval, float x);
bool is_front_face(vec3 ray_dir, vec3 outward_normal);
vec3 get_face_normal(vec3 outward_normal, bool is_front_face);
// Return a random.glsl value in section [0, 1).
float rand();
// Return a random.glsl value in section [minVal, maxVal).
float rand(float minVal, float maxVal);
vec3 rand_vec3(float min_val, float max_val);
vec3 rand_vec_in_unit_sphere();
vec3 rand_unit_vec();
vec3 rand_on_hemisphere(vec3 normal);
HitRecord hit_sphere(Ray ray, Sphere sphere, Interval ray_t);
bool hitAABB(Ray ray, AABB aabb, Interval ray_t);
vec3 pixel_sample_square();
vec3 lambertian_scatter(vec3 normal);
vec3 metal_scatter(vec3 ray_in_dir, vec3 normal, float fuzz);
vec3 refract_scatter(vec3 ray_in_dir, vec3 normal, float eta);

// Transform the passed in linear-space color to gamma space using gamma value of 2.
vec3 linear_to_gamma(vec3 linear_component) {
    // Gamma value of 2 will make the calculation a square root:
    return sqrt(linear_component);
}

bool near_zero(vec3 v) {
    float s = 1e-8;
    return abs(v.x) < s && abs(v.y) < s && abs(v.z) < s;
}

vec3 scatter(vec3 ray_in_dir, vec3 normal, bool is_front_face, float material) {
    switch (int(material)) {
        case MATERIAL_LAMBERTIAN: {
            should_scatter = true;
            return lambertian_scatter(normal);
        }
        case MATERIAL_METAL: {
            // The fuzz value is set in the floating point of the material variable, so:
            float fuzz = material - MATERIAL_METAL;
            vec3 scattered_dir = metal_scatter(ray_in_dir, normal, fuzz);
            should_scatter = dot(scattered_dir, normal) > 0.0; // check if the ray is absorbed by the metal
            return scattered_dir;
        }
        case MATERIAL_DIELECTRIC: {
            // The index of refraction is set from the 2nd digit in the floating point, so:
            float eta = (material - MATERIAL_DIELECTRIC) * 10.0;
            if(is_front_face) eta = 1.0 / eta;
            should_scatter = true;
            return refract_scatter(ray_in_dir, normal, eta);
        }
    }
}

bool is_sphere(float index) {
    float id = index - int(index);
    return interval_surrounds(Interval(0.001, 0.101), id);
}

HitRecord traceTroughBVH(Ray ray, Interval ray_t) {
    int stack[64];
    int stack_ptr = 0;
    stack[stack_ptr++] = 0;
    HitRecord hit_record;
    hit_record.hit = false;

    while (stack_ptr > 0) {
        int nodeIndex = stack[--stack_ptr];
        BVHNode node = bvh_nodes[nodeIndex];

        if (hitAABB(ray, node.bbox, ray_t)) {
            if (is_sphere(node.left_idx)) {
                Sphere sphere = spheres[int(node.left_idx)];
                HitRecord temp_rec;
                temp_rec = hit_sphere(ray, sphere, ray_t);
                if (temp_rec.hit) {
                    ray_t.max = temp_rec.t;
                    hit_record = temp_rec;
                    albedo = sphere.albedo;
                    material = sphere.material;
                }

                sphere = spheres[int(node.right_idx)];
                temp_rec = hit_sphere(ray, sphere, ray_t);
                if (temp_rec.hit) {
                    ray_t.max = temp_rec.t;
                    hit_record = temp_rec;
                    albedo = sphere.albedo;
                    material = sphere.material;
                }
            } else {
                int i_left = int(node.left_idx);
                int i_right = int(node.right_idx);

                bool hit_left = hitAABB(ray, bvh_nodes[i_left].bbox, ray_t);
                bool hit_right = hitAABB(ray, bvh_nodes[i_right].bbox, ray_t);

                if (hit_left)
                    stack[stack_ptr++] = i_left;
                if (hit_right)
                    stack[stack_ptr++] = i_right;
            }
        }
    }

    return hit_record;
}

vec3 get_norm_coord() {
    float aspect_ratio = image_size.x / image_size.y;

    // Normalize x and y into range [-viewport_width / 2, viewport_width / 2] and [-viewport_height / 2, viewport_height / 2].
    vec3 coord = up_left_pos;

    // X
    coord += pixel_coord.x * pixel_delta_u;
    // Y
    coord += pixel_coord.y * pixel_delta_v;

    // random offset (for multi-sampling)
    coord += pixel_sample_square();
    return coord;
}

Ray get_ray(vec3 normal_coord) {
    // Construct a camera ray originating from the defocus disk and directed at a randomly
    // sampled point around the pixel location.

    Ray ray;
    ray.o = (defocus_angle <= 0) ? camera_pos : defocus_disk_sample();;
    ray.dir = normal_coord - ray.o;
    ray.time = rand();

    return ray;
}

vec3 get_color(Ray ray) {
    vec3 color = vec3(0.0);
    vec3 color_scale = vec3(1.0);

    for (int i = 0; i < max_depth; i++) {
        HitRecord hit_record = traceTroughBVH(ray, Interval(0.001, INFINITY));

        if (hit_record.hit) {
            ray.dir = scatter(ray.dir, hit_record.normal, hit_record.is_front_face, material);
            // The scatter() function will also update the should_scatter global variable.

            if(should_scatter) {
                // Catch degenerate scatter direction.
                if (near_zero(ray.dir)) {
                    ray.dir = hit_record.normal;
                }
                ray.o = hit_record.p;
                color_scale *= albedo;
                continue;
            } else {
                return vec3(0.0);
            }
        }

        vec3 unit_direction = normalize(ray.dir);
        float a = 0.5 * (unit_direction.y + 1.0);
        color += ((1.0 - a) * vec3(1.0) + a * vec3(0.5, 0.7, 1.0)) * color_scale;
        break;
    }

    return linear_to_gamma(color);
}

void main() {
    pixel_coord = gl_GlobalInvocationID.xy;
    ivec2 i_pixel_coord = ivec2(pixel_coord);
    image_size = vec2(imageSize(img_output));

    // Get the color in the img_ouput object and mix it with the color of this raytrace.
    vec3 color = imageLoad(img_output, i_pixel_coord).rgb;
    if(last_color_scale == 0.0) {
        // Reset the color to zero if it's first sample.
        color *= 0.0;
    } else {
        color /= last_color_scale; // to x1 color
        color *= this_color_scale; // to x(1/render_count) color
    }
    Ray ray = get_ray(get_norm_coord());
    color += get_color(ray) * this_color_scale;

    imageStore(img_output, i_pixel_coord, vec4(color, 1.0));
}