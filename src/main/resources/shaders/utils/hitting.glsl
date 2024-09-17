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

bool interval_surrounds(Interval interval, float x);

bool is_front_face(vec3 ray_dir, vec3 outward_normal) {
    // The paseed in outward_normal should be unit vector.
    return dot(ray_dir, outward_normal) < 0.0;
}

vec3 get_face_normal(vec3 outward_normal, bool is_front_face) {
    return is_front_face ? outward_normal : -outward_normal;
}

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