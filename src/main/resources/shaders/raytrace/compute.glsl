#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;

layout (local_size_x = 16, local_size_y = 16) in;

const float INFINITY = 3.402823E+38;
const float MATERIAL_LAMBERTIAN = 0.0;
const float MATERIAL_METAL = 1.0;

uniform int u_sample_per_pixel; // Count random.glsl samples for each pixel.
uniform int u_max_depth;        // Maximum number of ray bounces into scene.

vec2 image_size;
vec2 pixel_coord;
vec2 pixel_delta_u;
vec2 pixel_delta_v;
float rand_factor = 0.0;

// The includes. Must be after the global variables because some of the includes use those.
#include <utils/interval.glsl>
#include <utils/random.glsl>
#include <utils/hitting.glsl>
#include <utils/scatter.glsl>

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

layout(std430, binding = 0) buffer DataBuffer {
    float spheres_count; // Count of spheres sent in from Java side.
    Sphere spheres[];
};

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
vec2 pixel_sample_square(int i);
vec3 lambertian_scatter(vec3 normal);
vec3 metal_scatter(vec3 ray_in_dir, vec3 normal);

// Transform the passed in linear-space color to gamma space using gamma value of 2.
vec3 linear_to_gamma(vec3 linear_component) {
    // Gamma value of 2 will make the calculation a square root:
    return sqrt(linear_component);
}

bool near_zero(vec3 v) {
    float s = 1e-8;
    return v.x < s && v.y < s && v.z < s;
}

vec3 scatter(vec3 ray_in_dir, vec3 normal, float material) {
    if(material == MATERIAL_LAMBERTIAN) return lambertian_scatter(normal);
    else if(material == MATERIAL_METAL) return metal_scatter(ray_in_dir, normal);
}

vec2 get_norm_coord(int i) {
    float aspect_ratio = image_size.x / image_size.y;

    vec2 pixel_coord_rand = pixel_coord + pixel_sample_square(i);

    // Get the normalized coordinate. For the case of aspect_ratio >= 1, y should be transformed into range [-1, 1]
    //, and x should have the same scale as y (translation should be scaled too).
    vec2 coord;
    coord.x = pixel_coord_rand.x / image_size.y * 2.0 - aspect_ratio;
    coord.y = -(pixel_coord_rand.y / image_size.y * 2.0 - 1.0);
    return coord;
}

Ray get_ray(vec2 normal_coord) {
    Ray ray;
    ray.o = vec3(0.0);
    ray.dir = normalize(vec3(normal_coord, -1.0));

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
            ray.dir = scatter(ray.dir, hit_record.normal, material);
            // Catch degenerate scatter direction.
            if(near_zero(ray.dir)) {
                ray.dir = hit_record.normal;
            }
            ray.o = hit_record.p;
            color_scale *= albedo;
            continue;
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
    pixel_delta_u = vec2(pixel_coord.x, 0.0) / image_size.x;
    pixel_delta_v = vec2(0.0, pixel_coord.y) / image_size.y;

    vec3 color = vec3(0.0);
    for (int i = 0; i < u_sample_per_pixel; i++) {
        Ray ray = get_ray(get_norm_coord(i));
        color += get_color(ray) / u_sample_per_pixel;
    }

    imageStore(img_output, ivec2(pixel_coord), vec4(color, 1.0));
}