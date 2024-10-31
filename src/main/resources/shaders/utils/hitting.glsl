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

struct Sphere {
    vec3 center1;
    int texture_id;
    vec3 center_vec;
    float radius;
    vec3 albedo;
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

bool interval_surrounds(Interval interval, float x);

bool is_front_face(vec3 ray_dir, vec3 outward_normal) {
    // The paseed in outward_normal should be unit vector.
    return dot(ray_dir, outward_normal) < 0.0;
}

vec3 get_face_normal(vec3 outward_normal, bool is_front_face) {
    return is_front_face ? outward_normal : -outward_normal;
}

vec3 sphere_center(vec3 center1, vec3 center_vec) {
    return center1 + center_vec * time;
}

bool hit_sphere(Ray ray, Interval ray_t, vec3 center1, vec3 center_vec, float radius, inout HitRecord hit_record) {
    vec3 center = sphere_center(center1, center_vec);
    vec3 oc = ray.o - center;
    float a = dot(ray.dir, ray.dir);
    float half_b = dot(oc, ray.dir);
    float c = dot(oc, oc) - radius * radius;
    float discriminant = half_b * half_b - a * c;

    if (discriminant < 0.0) {
        return false;
    } else {
        float sqrtd = sqrt(discriminant);

        // Find the nearest root that lies in the acceptable range.
        float root = (-half_b - sqrtd) / a;
        if (!interval_surrounds(ray_t, root)) {
            root = (-half_b + sqrtd) / a;
            if (!interval_surrounds(ray_t, root))
                return false;
        }

        hit_record.t = root;
        hit_record.p = ray.o + ray.dir * hit_record.t;
        vec3 outward_normal = (hit_record.p - center) / radius;
        hit_record.is_front_face = is_front_face(ray.dir, outward_normal);
        hit_record.normal = get_face_normal(outward_normal, hit_record.is_front_face);
        return true;
    }
}

Interval axis_interval(int n, AABB aabb) {
    if (n == 1) return aabb.y;
    if (n == 2) return aabb.z;
    return aabb.x;
}

bool hit_aabb(Ray ray, Interval ray_t,  AABB aabb) {
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