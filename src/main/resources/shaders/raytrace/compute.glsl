#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;

layout (local_size_x = 16, local_size_y = 16) in;

const float INFINITY = 3.402823E+38;
const int MATERIAL_LAMBERTIAN = 0;
const int MATERIAL_METAL = 1;
const int MATERIAL_DIELECTRIC = 2;

uniform int u_sample_per_pixel; // Count random.glsl samples for each pixel.
uniform int u_max_depth;        // Maximum number of ray bounces into scene.

vec2 image_size;
vec2 pixel_coord;
float rand_factor = 0.0;
bool should_scatter;

struct Ray {
    vec3 o;     // origin
    vec3 dir;   // direction
};

struct HitRecord {
    bool hit;
    bool is_front_face;
    vec3 p;
    vec3 normal;
    float t;
};

struct Sphere {
    vec3 center;
    float radius;
    vec3 albedo;
    float material;
};

struct Interval {
    float min;
    float max;
};

layout(std430, binding = 0) buffer ModelsBuffer {
    float spheres_count; // Count of spheres sent in from Java side.
    Sphere spheres[];
};

layout(std430, binding = 1) buffer CameraBuffer {
    float viewport_width;
    float viewport_height;
    float aspect_ratio;
    vec2 pixel_delta_u;
    vec2 pixel_delta_v;
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
vec2 pixel_sample_square();
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

vec2 get_norm_coord() {
    float aspect_ratio = image_size.x / image_size.y;

    // Normalize x and y into range [-viewport_width / 2, viewport_width / 2] and [-viewport_height / 2, viewport_height / 2].
    vec2 coord;

    // X
    coord.x = pixel_coord.x / image_size.x * viewport_width - viewport_width / 2.0;
    // Y
    coord.y = pixel_coord.y / image_size.y * viewport_height - viewport_height / 2.0;

    // random offset (for multi-sampling)
    coord += pixel_sample_square();
    return coord;
}

Ray get_ray(vec2 normal_coord) {
    Ray ray;
    ray.o = vec3(0.0);
    ray.dir = vec3(normal_coord, -1.0);

    return ray;
}

vec3 get_color(Ray ray) {
    vec3 color = vec3(0.0);
    vec3 color_scale = vec3(1.0);

    for (int i = 0; i < u_max_depth; i++) {
        HitRecord hit_record;
        float material;
        vec3 albedo;
        bool has_hit_anything = false;
        float nearest_so_far = INFINITY;
        for (int j = 0; j < spheres_count; j++) {
            hit_record = hit_sphere(ray, spheres[j], Interval(0.001, nearest_so_far));
            if (hit_record.hit) {
                material = spheres[j].material;
                albedo = spheres[j].albedo;
                nearest_so_far = hit_record.t;
                has_hit_anything = true;
            }
        }

        if (has_hit_anything) {
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
    image_size = vec2(imageSize(img_output));

    vec3 color = vec3(0.0);
    for (int i = 0; i < u_sample_per_pixel; i++) {
        Ray ray = get_ray(get_norm_coord());
        color += get_color(ray) / u_sample_per_pixel;
    }

    imageStore(img_output, ivec2(pixel_coord), vec4(color, 1.0));
}