#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;

layout (local_size_x = 16, local_size_y = 16) in;

const float INFINITY = 3.402823E+38;
const int MATERIAL_LAMBERTIAN = 0;
const int MATERIAL_METAL = 1;
const int MATERIAL_DIELECTRIC = 2;
const int MATERIAL_DIFFUSE_LIGHT = 3;
const int MATERIAL_ISOTROPIC = 4;
const int PERLIN_POINT_COUNT = 256;
const int MODEL_SPHERE = 1;
const int MODEL_QUAD = 2;
const int MODEL_CONSTANT_MEDIUM = 3;
const int MODEL_BOX = 4;

// Values for extracting the IOR/eta from  material_val.
const float MIN_IOR = 1.0;
const float MAX_IOR = 2.5;

uniform sampler2D textures[8];
uniform int max_depth;        // Maximum number of ray bounces into scene.
uniform int frame_count; // The accumulated frame count.
uniform float u_rand_factor; // The initial random vector. This is for varying randomness from call to call.
uniform vec3 background; // The background color of the scene.
uniform float sqrt_spp; // Number of samples per pixel.
uniform float recip_sqrt_spp; // 1 / sqrt_spp.

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
vec3 attenuation;
vec3 color_from_emission;
int material;

struct Ray {
    vec3 o;     // origin
    vec3 dir;   // direction
};

struct HitRecord {
    bool is_front_face; // a boolean shows that whether this hit hits the front face of the object.
    vec3 p; // the hit point.
    vec3 normal; // the normal at the hit point of the object.
    float t; // the scale of the ray from its origin to the hit point.
    vec2 uv; // the uv coordinate for texture mapping.
};

struct Sphere {
    vec3 center1;

    // The texture information. The upper 16 bits stores the index in the texture uniform array. The lower 16 bits
    // stores whatever detail values.
    int texture_id;
    vec3 center_vec;
    float radius;

    vec3 emission;

    // The material value is a packed value. The upper 16 bits stores the model id, and the lower 16 bits stores the
    // data for varying information according to the material type. For example, metal material has its fuzz value in
    // the lower 16 bits.
    int material;
};

struct Quad {
    vec3 normal; // the normal of the plane.
    float d; // the d in equation ax+by+cz = d.
    vec3 q; // the origin for u and v.
    int material; // the packed material value.
    vec3 u; // a component vector that structs the quad.
    int texture_id; // texture information
    vec3 v; // a component vector that structs the quad.
    float area;
    vec3 emission;
};

struct Box{
    Quad quads[6];
};

struct ConstantMedium {
    int boundary_model_idx;
    int boundary_model_type;
    float neg_inv_density;
    int phase_function; // a material packed value.
    int texture_id;
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

layout(std430, binding = 0) buffer SphereBuffer {
    Sphere spheres[];
};

layout(std430, binding = 1) buffer BVHBuffer {
    BVHNode bvh_nodes[];
};

layout(std430, binding = 2) buffer QuadBuffer {
    Quad quads[];
};

layout(std430, binding = 3) buffer ConstantMediumBuffer {
    ConstantMedium constant_mediums[];
};

layout(std430, binding = 4) buffer BoxBuffer {
    Box boxes[];
};

layout(std430, binding = 5) buffer LightsBuffer {
    int lights_count;

    // The packed values. The upper 16 bits store the model type, and
    // the lower 16 bits store the model index in their buffers.
    int lights[];
};

// The includes. Must be after the global variables and ssbos because some of the includes use those.
#include <utils/math.glsl>
#include <utils/interval.glsl>
#include <utils/random.glsl>
#include <utils/texture.glsl>
#include <utils/hitting.glsl>
#include <utils/pdf.glsl>
#include <utils/scatter.glsl>

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
bool hit_sphere(Ray ray, Interval ray_t, vec3 center1, vec3 center_vec, float radius, inout HitRecord hit_record);
bool hit_aabb(Ray ray, Interval ray_t,  AABB aabb);
vec3 pixel_sample_square();
vec3 lambertian_scatter(vec3 normal);
void metal_scatter(inout vec3 ray_dir, vec3 normal, float fuzz);
void refract_scatter(inout vec3 ray_dir, vec3 normal, float eta);
void isotropic_scatter(inout Ray ray, vec3 p);
bool scatter(inout Ray ray, vec3 hit_point, vec3 normal, bool is_front_face, int material_val, out bool scatter_pdf);
vec3 checkerboard(vec3 p);
vec3 texture_color(vec3 p, int id, vec2 uv);
bool hit_model(Ray ray, Interval ray_t, int model_idx, int model_type, inout HitRecord hit_record);
float scattering_pdf(vec3 normal, vec3 scatter_dir, int material);
float quad_pdf_value(vec3 origin, vec3 direction, Quad quad);
vec3 quad_random(vec3 origin, Quad quad);
float material_pdf_value(vec3 direction, int material, vec3 normal);

int get_node_type(int id) {
    id &= 0xFFFF; // extract value
    return id;
}

void set_material_properties(int model_idx, int model_type, vec3 p, vec2 uv, bool is_front_face) {
    switch(model_type) {
        case 1: // sphere
            Sphere sphere = spheres[model_idx];
            material = sphere.material;
            attenuation = texture_color(p, sphere.texture_id, uv);
            color_from_emission = is_front_face ? sphere.emission : vec3(0.0);
            return;
        case 2: // quad
            Quad quad = quads[model_idx];
            material = quad.material;
            attenuation = texture_color(p, quad.texture_id, uv);
            color_from_emission = is_front_face ? quad.emission : vec3(0.0);
            return;
        case 3: // constant medium
            ConstantMedium medium = constant_mediums[model_idx];
            material = medium.phase_function;
            attenuation = texture_color(p, medium.texture_id, uv);
            color_from_emission = vec3(0.0);
            return;
        case 4: // box
            Box box = boxes[model_idx];
            material = box.quads[0].material;
            attenuation = texture_color(p, box.quads[0].texture_id, uv);
            color_from_emission = is_front_face ? box.quads[0].emission : vec3(0.0);
            return;
    }
}

bool trace_through_bvh(Ray ray, Interval ray_t, out HitRecord hit_record) {
    int node_idx;
    int model_idx;
    int stack[64];
    int stack_ptr = 0;
    stack[stack_ptr++] = 0;

    BVHNode node;
    bool has_hit = false;

    while (stack_ptr > 0) {
        node_idx = stack[--stack_ptr];
        node = bvh_nodes[node_idx];

        if (hit_aabb(ray, ray_t, node.bbox)) {
            int node_type = get_node_type(node.left_id);
            if (node_type != 0) { // if left is leaf, right should also be leaf.
                // Test left and right models.

                // Extract the sphere index from the upper 16 bits.
                model_idx = (node.left_id >> 16) & 0xFFFF;
                for (int i = 0; i < 2; i++) {
                    if (hit_model(ray, ray_t, model_idx, node_type, hit_record)) {
                        has_hit = true;
                        ray_t.max = hit_record.t;

                        set_material_properties(model_idx, node_type, hit_record.p, hit_record.uv, hit_record.is_front_face);
                    }
                    model_idx = (node.right_id >> 16) & 0xFFFF;
                    node_type = get_node_type(node.right_id);
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

    // Offsets the vector to a random point in the square sub-pixel specified by grid for an idealized unit square pixel
    // [-.5,-.5] to [+.5,+.5].
    coord += pixel_sample_square();
    return coord;
}

Ray get_ray() {
    // Construct a camera ray originating from the defocus disk and directed at a randomly
    // sampled point around the pixel location.

    vec3 normal_coord = get_norm_coord();

    Ray ray;
    ray.o = (defocus_angle <= 0) ? camera_pos : defocus_disk_sample();;
    ray.dir = normal_coord - ray.o;

    return ray;
}

vec3 ray_color(Ray ray) {
    vec3 final_color = vec3(0.0);
    vec3 accumulated_attenuation = vec3(1.0); // Start with no attenuation

    HitRecord hit_record;
    // Loop until we either reach the maximum recursion depth or stop scattering.
    for (int i = 0; i < max_depth; i++) {
        if (!trace_through_bvh(ray, Interval(0.001, INFINITY), hit_record)) {
            final_color = accumulated_attenuation * background;
            break;
        }

        bool skip_pdf;

        if(!scatter(ray, hit_record.p, hit_record.normal, hit_record.is_front_face, material, skip_pdf)) {
            final_color = accumulated_attenuation * color_from_emission;
            break;
        }

        // Update the ray origin to the hit point.
        ray.o = hit_record.p;

        if (skip_pdf) {
            accumulated_attenuation *= attenuation;
            continue;
        }

        // The book uses a class to handle mixture of pdfs. But since we're writing in a non-object-oriented language,
        // I'll just write the code the way below to do the same thing. Hopefully it's also clear enough.
        if (rand() < 0.5) ray.dir = lights_random(ray.o);
        float lights_pdf_value = lights_pdf_value(ray.o, ray.dir);
        float pdf_value = 0.5 * lights_pdf_value + 0.5 * material_pdf_value(ray.dir, material, hit_record.normal);

        // If the PDF value is zero, the direction is invalid.
        if(pdf_value == 0.0) {
            final_color = accumulated_attenuation * color_from_emission;
            break;
        }

        float scattering_pdf = scattering_pdf(hit_record.normal, ray.dir, material);

        accumulated_attenuation *= attenuation * scattering_pdf / pdf_value;
    }

    return final_color;
}

void main() {
    pixel_coord = gl_GlobalInvocationID.xy;
    ivec2 i_pixel_coord = ivec2(pixel_coord);
    image_size = vec2(imageSize(img_output));
    time = rand();
    Ray ray = get_ray();

    // Get the color in the img_ouput object and mix it with the color of this raytrace.
    vec3 previous_color = imageLoad(img_output, i_pixel_coord).rgb;
    vec3 current_color = ray_color(ray);
    vec3 accumulated_color = (previous_color * float(frame_count - 1) + current_color) / float(frame_count);

    imageStore(img_output, i_pixel_coord, vec4(accumulated_color, 1.0));
}