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

bool interval_surrounds(Interval interval, float x);

bool is_front_face(vec3 ray_dir, vec3 outward_normal) {
    // The paseed in outward_normal should be unit vector.
    return dot(ray_dir, outward_normal) < 0.0;
}

vec3 get_face_normal(vec3 outward_normal, bool is_front_face) {
    return is_front_face ? outward_normal : -outward_normal;
}

vec3 sphere_center(vec3 center1, vec3 center_vec, float time) {
    return center1 + center_vec * time;
}

HitRecord hit_sphere(Ray ray, Sphere sphere, Interval ray_t) {
    vec3 center = sphere_center(sphere.center1, sphere.center_vec, ray.time);
    vec3 oc = ray.o - center;
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
        hit_record.p = ray.o + ray.dir * hit_record.t;
        vec3 outward_normal = (hit_record.p - center) / sphere.radius;
        hit_record.is_front_face = is_front_face(ray.dir, outward_normal);
        hit_record.normal = get_face_normal(outward_normal, hit_record.is_front_face);
        return hit_record;
    }
}

Interval axis_interval(int n, AABB aabb) {
    if (n == 1) return aabb.y;
    if (n == 2) return aabb.z;
    return aabb.x;
}

bool hitAABB(Ray ray, AABB aabb, Interval ray_t) {
    for (int axis = 0; axis < 3; axis++) {
        Interval ax = axis_interval(axis, aabb);
        float adinv = 1.0 / ray.dir[axis];

        float t0 = (ax.min - ray.o[axis]) * adinv;
        float t1 = (ax.max - ray.o[axis]) * adinv;

        if (t0 < t1) {
            if (t0 > ray_t.min) ray_t.min = t0;
            if (t1 < ray_t.max) ray_t.max = t1;
        } else {
            if (t1 > ray_t.min) ray_t.min = t1;
            if (t0 < ray_t.max) ray_t.max = t0;
        }

        if (ray_t.max <= ray_t.min)
            return false;
    }

    return true;
}