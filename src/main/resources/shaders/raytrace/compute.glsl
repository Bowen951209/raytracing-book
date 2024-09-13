#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;


layout (local_size_x = 16, local_size_y = 16) in;

const float INFINITY = 3.402823E+38;

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


bool is_front_face(vec3 ray_dir, vec3 outward_normal) {
    // The paseed in outward_normal should be unit vector.
    return dot(ray_dir, outward_normal) < 0.0;
}

vec3 get_face_normal(vec3 outward_normal, bool is_front_face) {
    return is_front_face ? outward_normal : -outward_normal;
}

// Return the distance from the ray to the sphere.
HitRecord hit_sphere(Ray ray, Sphere sphere, float ray_tmin, float ray_tmax) {
    vec3 oc = ray.o - sphere.center;
    float a = dot(ray.dir, ray.dir);
    float half_b = dot(oc, ray.dir);
    float c = dot(oc, oc) - sphere.radius * sphere.radius;
    float discriminant = half_b * half_b - a * c;

    HitRecord hit_record;
    if(discriminant < 0.0) {
        hit_record.hit = false;
        return hit_record;
    } else {
        float sqrtd = sqrt(discriminant);

        // Find the nearest root that lies in the acceptable range.
        float root = (-half_b - sqrtd) / a;
        if(root <= ray_tmin || ray_tmax <= root) {
            root = (-half_b + sqrtd) / a;
            if(root <= ray_tmin || ray_tmax <= root){
                hit_record.hit = false;
                return hit_record;
            }
        }

        hit_record.hit = true;
        hit_record.t = root;
        hit_record.p = ray.o + ray.dir * hit_record.t;
        vec3 outward_normal = (hit_record.p - sphere.center) / sphere.radius;
        hit_record.is_front_face = is_front_face(ray.dir, outward_normal);
        hit_record.normal = get_face_normal(outward_normal, hit_record.is_front_face);
        return hit_record;
    }
}

vec2 get_norm_coord(vec2 pixel_coord) {
    vec2 img_size = vec2(imageSize(img_output));
    float aspect_ratio = img_size.x / img_size.y;

    // Get the normalized coordinate. For the case of aspect_ratio >= 1, y should be transformed into range [-1, 1]
    //, and x should have the same scale as y (translation should be scaled too).
    vec2 coord;
    coord.x = pixel_coord.x / img_size.y * 2.0 - aspect_ratio;
    coord.y = -(pixel_coord.y / img_size.y * 2.0 - 1.0);
    return coord;
}

Ray get_ray(vec2 normal_coord) {
    Ray ray;
    ray.o = vec3(0.0);
    ray.dir = vec3(normal_coord, -1.0);

    return ray;
}

vec3 get_color(Ray ray) {
    HitRecord hit_record = hit_sphere(ray, Sphere(vec3(0.0, 0.0, -1.0), 0.5), 0.0, INFINITY);
    if(hit_record.hit) {
        vec3 n = hit_record.normal;
        return 0.5 * vec3(n.x + 1.0, n.y + 1.0, n.z + 1.0);
    }

    vec3 unit_direction = normalize(ray.dir);
    float a = 0.5 * (unit_direction.y + 1.0);
    return (1.0 - a) * vec3(1.0) + a * vec3(0.5, 0.7, 1.0);
}

void main() {
    ivec2 pixel_coord = ivec2(gl_GlobalInvocationID.xy);
    Ray ray = get_ray(get_norm_coord(vec2(pixel_coord)));
    imageStore(img_output, pixel_coord, vec4(get_color(ray), 1.0));
}