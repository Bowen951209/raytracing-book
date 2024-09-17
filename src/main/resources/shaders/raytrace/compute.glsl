#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;


layout (local_size_x = 16, local_size_y = 16) in;

const float INFINITY = 3.402823E+38;

uniform int u_sample_per_pixel; // Count random samples for each pixel.
uniform int u_max_depth;        // Maximum number of ray bounces into scene.

vec2 image_size;
vec2 pixel_coord;
vec2 pixel_delta_u;
vec2 pixel_delta_v;
float rand_factor = 0.0;
float closest_so_far = 0.0;

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
};

struct Interval {
    float min;
    float max;
};

layout(std430, binding = 0) buffer DataBuffer {
    Sphere spheres[];
};

bool interval_surrounds(Interval interval, float x) {
    return interval.min < x && x < interval.max;
}

bool is_front_face(vec3 ray_dir, vec3 outward_normal) {
    // The paseed in outward_normal should be unit vector.
    return dot(ray_dir, outward_normal) < 0.0;
}

vec3 get_face_normal(vec3 outward_normal, bool is_front_face) {
    return is_front_face ? outward_normal : -outward_normal;
}

// Return a random value in section [0, 1).
float rand() {
    rand_factor += 0.001;
    vec2 co = pixel_coord;
    co += rand_factor;
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// Return a random value in section [minVal, maxVal).
float rand(float minVal, float maxVal) {
    return minVal + rand() * (maxVal - minVal);
}

vec3 rand_vec3(float min_val, float max_val) {
    float rand1 = rand(min_val, max_val);
    float rand2 = rand(min_val, max_val);
    float rand3 = rand(min_val, max_val);
    return vec3(rand1, rand2, rand3);
}

vec3 rand_vec_in_unit_sphere() {
    while (true) {
        vec3 p = rand_vec3(-1.0, 1.0);
        if (dot(p, p) < 1) return p;
    }
}

vec3 rand_unit_vec() {
    return normalize(rand_vec_in_unit_sphere());
}

vec3 rand_on_hemisphere(vec3 normal) {
    vec3 on_unit_sphere = rand_unit_vec();
    if(dot(on_unit_sphere, normal) > 0.0) { // In the same hemisphere as the normal
        return on_unit_sphere;
    }
    return -on_unit_sphere;
}

// Transform the passed in linear-space color to gamma space using gamma value of 2.
vec3 linear_to_gamma(vec3 linear_component) {
    // Gamma value of 2 will make the calculation a square root:
    return sqrt(linear_component);
}

// Return the distance from the ray to the sphere.
HitRecord hit_sphere(Ray ray, Sphere sphere, Interval ray_t) {
    vec3 oc = ray.o - sphere.center;
    float a = dot(ray.dir, ray.dir);
    float half_b = dot(oc, ray.dir);
    float c = dot(oc, oc) - sphere.radius * sphere.radius;
    float discriminant = half_b * half_b - a * c;

    HitRecord hit_record;
    if (discriminant < 0.0) {
        hit_record.hit = false;
        return hit_record;
    } else {
        float sqrtd = sqrt(discriminant);

        // Find the nearest root that lies in the acceptable range.
        float root = (-half_b - sqrtd) / a;
        if (!interval_surrounds(ray_t, root)) {
            root = (-half_b + sqrtd) / a;
            if (!interval_surrounds(ray_t, root)) {
                hit_record.hit = false;
                return hit_record;
            }
        }

        hit_record.hit = true;
        hit_record.t = root;
        closest_so_far = hit_record.t;
        hit_record.p = ray.o + ray.dir * hit_record.t;
        vec3 outward_normal = (hit_record.p - sphere.center) / sphere.radius;
        hit_record.is_front_face = is_front_face(ray.dir, outward_normal);
        hit_record.normal = get_face_normal(outward_normal, hit_record.is_front_face);
        return hit_record;
    }
}

vec2 pixel_sample_square(int i) {
    float px = -0.5 + rand();
    float py = -0.5 + rand();
    return px * pixel_delta_u + py * pixel_delta_v;
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
    float color_scale = 1.0;

    for (int i = 0; i < u_max_depth; i++) {
        HitRecord hit_record;
        for (int j = 0; j < 2; j++) {
            hit_record = hit_sphere(ray, spheres[j], Interval(0.001, INFINITY));
            if (hit_record.hit) break;
        }

        if (hit_record.hit) {
            ray.dir = hit_record.normal + rand_unit_vec();
            ray.o = hit_record.p;
            color_scale *= 0.5;
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

    vec3 color;
    for (int i = 0; i < u_sample_per_pixel; i++) {
        Ray ray = get_ray(get_norm_coord(i));
        color += get_color(ray) / u_sample_per_pixel;
    }

    imageStore(img_output, ivec2(pixel_coord), vec4(color, 1.0));
}