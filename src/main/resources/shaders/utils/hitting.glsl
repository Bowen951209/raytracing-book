const int MODEL_SPHERE = 1;
const int MODEL_QUAD = 2;
const int MODEL_BOX = 4;

struct Ray {
    vec3 o;     // origin
    vec3 dir;   // direction
};

struct HitRecord {
    bool is_front_face;
    vec3 p;
    vec3 normal;
    float t;
    vec2 uv;
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
bool interval_contains(Interval interval, float x);

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
        hit_record.uv = get_sphere_uv(hit_record.p);

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

bool is_interior(float a, float b, out vec2 uv) {
    Interval unit_interval = Interval(0, 1);
    // Given the hit point in plane coordinates, return false if it is outside the
    // primitive, otherwise set the hit record UV coordinates and return true.

    if (!interval_contains(unit_interval, a) || !interval_contains(unit_interval, b))
        return false;

    uv = vec2(a, b);
    return true;
}

bool hit_quad(Ray ray, Interval ray_t, vec3 normal, vec3 q, vec3 u, vec3 v, float d, inout HitRecord hit_record) {
    float denom = dot(normal, ray.dir);

    // No hit if the ray is parallel to the plane.
    if (abs(denom) < 1e-8)
        return false;

    // Return false if the hit point parameter t is outside the ray interval.
    float t = (d - dot(normal, ray.o)) / denom;
    if (!interval_contains(ray_t, t))
        return false;

    // Determine if the hit point lies within the planar shape using its plane coordinates.
    vec3 intersection = ray.o + ray.dir * t;
    vec3 planar_hitpt_vector = intersection - q;

    float delta, alpha, beta;

    if((delta = u.x * v.y - u.y * v.x) != 0) {
        alpha = (planar_hitpt_vector.x * v.y - planar_hitpt_vector.y * v.x) / delta;
        beta = (planar_hitpt_vector.y * u.x - planar_hitpt_vector.x * u.y) / delta;
    } else if((delta = u.x * v.z - u.z * v.x) != 0) {
        alpha = (planar_hitpt_vector.x * v.z - planar_hitpt_vector.z * v.x) / delta;
        beta = (planar_hitpt_vector.z * u.x - planar_hitpt_vector.x * u.z) / delta;
    } else {
        delta = u.y * v.z - u.z * v.y;
        alpha = (planar_hitpt_vector.y * v.z - planar_hitpt_vector.z * v.y) / delta;
        beta = (planar_hitpt_vector.z * u.y - planar_hitpt_vector.y * u.z) / delta;
    }

    if (!is_interior(alpha, beta, hit_record.uv))
        return false;

    // Ray hits the 2D shape; set the rest of the hit record and return true.
    hit_record.t = t;
    hit_record.p = intersection;
    hit_record.is_front_face = is_front_face(ray.dir, normal);
    hit_record.normal = get_face_normal(normal, hit_record.is_front_face);


bool hit_box(Ray ray, Interval ray_t, Box box, inout HitRecord hit_record) {
    bool has_hit = false;
    // Check through a box's 6 sides.
    for (int i = 0; i < 6; i++) {
        Quad quad = box.quads[i];
        if (hit_quad(ray, ray_t, quad.normal, quad.q, quad.u, quad.v, quad.d, hit_record)) {
            ray_t.max = hit_record.t;
            has_hit = true;
        }
    }

    return has_hit;
}

bool hit_boundary(Ray ray, Interval ray_t, int model_idx, int model_type, inout HitRecord hit_record) {
    switch(model_type) {
        case MODEL_SPHERE:
            Sphere sphere = spheres[model_idx];
            return hit_sphere(ray, ray_t, sphere.center1, sphere.center_vec, sphere.radius, hit_record);
        case MODEL_QUAD:
            Quad quad = quads[model_idx];
            return hit_quad(ray, ray_t, quad.normal, quad.q, quad.u, quad.v, quad.d, hit_record);
        case MODEL_BOX:
            Box box = boxes[model_idx];
            return hit_box(ray, ray_t, box, hit_record);
        default:
            return false;
    }
}

    return true;
}

bool hit_model(Ray ray, Interval ray_t, int model_idx, int model_type, inout HitRecord hit_record) {
    switch(model_type) {
        case MODEL_SPHERE:
            Sphere sphere = spheres[model_idx];
            return hit_sphere(ray, ray_t, sphere.center1, sphere.center_vec, sphere.radius, hit_record);
        case MODEL_QUAD:
            Quad quad = quads[model_idx];
            return hit_quad(ray, ray_t, quad.normal, quad.q, quad.u, quad.v, quad.d, hit_record);
        case MODEL_BOX:
            Box box = boxes[model_idx];
            return hit_box(ray, ray_t, box, hit_record);
        default:
            return false;
    }
}