#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;

layout (local_size_x = 16, local_size_y = 16) in;

const float INFINITY = 3.402823E+38;
const int MATERIAL_LAMBERTIAN = 0;
const int MATERIAL_METAL = 1;
const int MATERIAL_DIELECTRIC = 2;

// Values for extracting the IOR/eta from  material_val.
const float MIN_IOR = 1.0;
const float MAX_IOR = 2.5;

uniform sampler2D textures[8];
uniform int sample_per_pixel; // Count of samples for each pixel.
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
float time; // a factor that is in range [0, 1).
vec3 albedo;
int material;

struct Ray {
    vec3 o;     // origin
    vec3 dir;   // direction
};

struct HitRecord {
    bool is_front_face;
    vec3 p;
    vec3 normal;
    float t;
};

// TODO: Make structs for different model types to avoid memory wastes.
struct Sphere {
    vec3 center1;

    // The texture information. The upper 16 bits stores the index in the texture uniform array. The lower 16 bits
    // stores whatever detail values.
    int texture_id;
    vec3 center_vec;
    float radius;
    vec3 albedo;

    // The material value is a packed value. The upper 16 bits stores the model id, and the lower 16 bits stores the
    // data for varying information according to the material type. For example, metal material has its fuzz value in
    // the lower 16 bits.
    int material;
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

    // Left and right ids are packed values. The upper 16 bits stores the model index in its SSBO, and the lower 16 bits
    // stores the model id(0 for BVH node; 1 for spheres).
    // Note that value A and B only range [0, 65535].
    int left_id;
    int right_id;
};

layout(std430, binding = 0) buffer ModelsBuffer {
    Sphere spheres[];
};

layout(std430, binding = 1) buffer BVHBuffer {
    BVHNode bvh_nodes[];
};

// The includes. Must be after the global variables and ssbos because some of the includes use those.
#include <utils/math.glsl>
#include <utils/interval.glsl>
#include <utils/random.glsl>
#include <utils/hitting.glsl>
#include <utils/scatter.glsl>
#include <utils/texture.glsl>

// The placeholders for the functions in the includes.
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
bool hit_sphere(Ray ray, vec3 center1, vec3 center_vec, float radius, Interval ray_t, inout HitRecord hit_record);
bool hit_aabb(Ray ray, AABB aabb, Interval ray_t);
vec3 pixel_sample_square();
vec3 lambertian_scatter(vec3 normal);
void metal_scatter(inout vec3 ray_dir, vec3 normal, float fuzz);
void refract_scatter(inout vec3 ray_dir, vec3 normal, float eta);
vec3 checkerboard(vec3 p);
vec3 texture_color(vec3 p, int id);

// Transform the passed in linear-space color to gamma space using gamma value of 2.
vec3 linear_to_gamma(vec3 linear_component) {
    // Gamma value of 2 will make the calculation a square root:
    return sqrt(linear_component);
}

bool near_zero(vec3 v) {
    float s = 1e-8;
    return abs(v.x) < s && abs(v.y) < s && abs(v.z) < s;
}

bool scatter(inout vec3 ray_dir, vec3 normal, bool is_front_face, int material_val) {
    // Extract material ID from the upper 16 bits
    int material_id = (material_val >> 16) & 0xFFFF;

    switch (material_id) {
        case MATERIAL_LAMBERTIAN: {
            ray_dir = lambertian_scatter(normal);
            return true;
        }
        case MATERIAL_METAL: {
            // Extract fuzz from the lower 16 bits
            int fuzz_quantized = material_val & 0xFFFF;

            // Convert fuzz back to float (undo the quantization)
            float fuzz = float(fuzz_quantized) / 65535.0;

            metal_scatter(ray_dir, normal, fuzz);
            return dot(ray_dir, normal) > 0.0; // check if the ray is absorbed by the metal
        }
        case MATERIAL_DIELECTRIC: {
            // Extract quantized IOR from the lower 16 bits
            int iorQuantized = material_val & 0xFFFF;

            // Convert quantized IOR back to normalized float [0, 1]
            float normalizedIOR = float(iorQuantized) / 65535.0;

            // Scale back to the original IOR range [1.0, 2.5]
            float eta = mix(MIN_IOR, MAX_IOR, normalizedIOR);

            if(is_front_face) eta = 1.0 / eta;
            refract_scatter(ray_dir, normal, eta);
            return true;
        }
    }
}

bool is_sphere(int id) {
    // Extract the id from the lower 16 bits.
    id &= 0xFFFF;
    return id == 1; // 1 is the id of sphere
}

bool trace_through_bvh(Ray ray, Interval ray_t, out HitRecord hit_record) {
    int node_idx;
    int sphere_idx;
    int stack[64];
    int stack_ptr = 0;
    stack[stack_ptr++] = 0;

    BVHNode node;
    Sphere sphere;
    bool has_hit = false;

    while (stack_ptr > 0) {
        node_idx = stack[--stack_ptr];
        node = bvh_nodes[node_idx];

        if (hit_aabb(ray, node.bbox, ray_t)) {
            if (is_sphere(node.left_id)) { // if left is sphere, right should also be sphere.
                // Test left and right spheres.

                // Extract the sphere index from the upper 16 bits.
                sphere_idx = (node.left_id >> 16) & 0xFFFF;
                sphere = spheres[sphere_idx];
                for (int i = 0; i < 2; i++) {
                    sphere = spheres[sphere_idx];
                    if (hit_sphere(ray, sphere.center1, sphere.center_vec, sphere.radius, ray_t, hit_record)) {
                        has_hit = true;
                        ray_t.max = hit_record.t;
                        material = sphere.material;

                        // If no texture is specified, use albedo values, else get the texture color.
                        if(sphere.texture_id == -1)
                            albedo = sphere.albedo;
                        else
                            albedo = texture_color(hit_record.p, sphere.texture_id);
                    }
                    sphere_idx = (node.right_id >> 16) & 0xFFFF;
                }
            } else {
                // Extract the sphere indices from the upper 16 bits.
                stack[stack_ptr++] = (node.left_id >> 16) & 0xFFFF;
                stack[stack_ptr++] = (node.right_id >> 16) & 0xFFFF;
            }
        }
    }

    return has_hit;
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

    return ray;
}

vec3 get_color(Ray ray) {
    vec3 color = vec3(0.0);
    vec3 color_scale = vec3(1.0);

    HitRecord hit_record;
    for (int i = 0; i < max_depth; i++) {
        if (trace_through_bvh(ray, Interval(0.001, INFINITY), hit_record)) {
            if(scatter(ray.dir, hit_record.normal, hit_record.is_front_face, material)) {
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

        ray.dir = normalize(ray.dir);
        float a = 0.5 * (ray.dir.y + 1.0);
        color += ((1.0 - a) * vec3(1.0) + a * vec3(0.5, 0.7, 1.0)) * color_scale;
        break;
    }

    return linear_to_gamma(color);
}

void main() {
    pixel_coord = gl_GlobalInvocationID.xy;
    ivec2 i_pixel_coord = ivec2(pixel_coord);
    image_size = vec2(imageSize(img_output));
    time = rand();

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