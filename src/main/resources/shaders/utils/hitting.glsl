const int MODEL_SPHERE = 1;
const int MODEL_QUAD = 2;
const int MODEL_CONSTANT_MEDIUM = 3;
const int MODEL_BOX = 4;

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

bool hit_sphere(Ray ray, Interval ray_t, Sphere sphere, inout HitRecord hit_record) {
    vec3 center = sphere_center(sphere.center1, sphere.center_vec);
    vec3 oc = ray.o - center;
    float a = dot(ray.dir, ray.dir);
    float half_b = dot(oc, ray.dir);
    float c = dot(oc, oc) - sphere.radius * sphere.radius;
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
        vec3 outward_normal = (hit_record.p - center) / sphere.radius;
        hit_record.is_front_face = is_front_face(ray.dir, outward_normal);
        hit_record.normal = get_face_normal(outward_normal, hit_record.is_front_face);
        vec2 uv = get_sphere_uv(hit_record.p - center);

        material = sphere.material;
        albedo = texture_color(hit_record.p, sphere.texture_id, uv);
        color_from_emission = sphere.emission;

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

bool hit_quad(Ray ray, Interval ray_t, Quad quad, inout HitRecord hit_record) {
    vec3 normal = quad.normal;
    float denom = dot(normal, ray.dir);

    // No hit if the ray is parallel to the plane.
    if (abs(denom) < 1e-8)
        return false;

    // Return false if the hit point parameter t is outside the ray interval.
    float t = (quad.d - dot(normal, ray.o)) / denom;
    if (!interval_contains(ray_t, t))
        return false;

    // Determine if the hit point lies within the planar shape using its plane coordinates.
    vec3 intersection = ray.o + ray.dir * t;
    vec3 planar_hitpt_vector = intersection - quad.q;

    float delta, alpha, beta;

    vec3 u = quad.u;
    vec3 v = quad.v;
    if((delta = u.x * v.y - u.y * v.x) != 0) {
        alpha = (planar_hitpt_vector.x * v.y - planar_hitpt_vector.y * v.x) / delta;
        beta = (planar_hitpt_vector.y * u.x - planar_hitpt_vector.x * u.y) / delta;
    } else if ((delta = u.x * v.z - u.z * v.x) != 0) {
        alpha = (planar_hitpt_vector.x * v.z - planar_hitpt_vector.z * v.x) / delta;
        beta = (planar_hitpt_vector.z * u.x - planar_hitpt_vector.x * u.z) / delta;
    } else {
        delta = u.y * v.z - u.z * v.y;
        alpha = (planar_hitpt_vector.y * v.z - planar_hitpt_vector.z * v.y) / delta;
        beta = (planar_hitpt_vector.z * u.y - planar_hitpt_vector.y * u.z) / delta;
    }

    vec2 uv;
    if (!is_interior(alpha, beta, uv))
        return false;

    // Ray hits the 2D shape; set the rest of the hit record and return true.
    hit_record.t = t;
    hit_record.p = intersection;
    hit_record.is_front_face = is_front_face(ray.dir, normal);
    hit_record.normal = get_face_normal(normal, hit_record.is_front_face);

    material = quad.material;
    albedo = texture_color(hit_record.p, quad.texture_id, uv);
    color_from_emission = quad.emission;

    return true;
}

bool hit_box(Ray ray, Interval ray_t, Box box, inout HitRecord hit_record){
    bool has_hit = false;
    // Check through a box's 6 sides.
    for (int i = 0; i < 6; i++) {
        if (hit_quad(ray, ray_t, box.quads[i], hit_record)) {
            ray_t.max = hit_record.t;
            has_hit = true;
        }
    }

    return has_hit;
}

bool hit_boundary(Ray ray, Interval ray_t, int model_idx, int model_type, inout HitRecord hit_record) {
    switch(model_type) {
        case MODEL_SPHERE:
            return hit_sphere(ray, ray_t, spheres[model_idx], hit_record);
        case MODEL_QUAD:
            return hit_quad(ray, ray_t, quads[model_idx], hit_record);
        case MODEL_BOX:
            Box box = boxes[model_idx];
            return hit_box(ray, ray_t, box, hit_record);
        default:
            return false;
    }
}

bool hit_constant_medium(Ray ray, Interval ray_t, ConstantMedium medium, inout HitRecord hit_record) {
    HitRecord rec1, rec2;

    if (!hit_boundary(ray, Interval(-INFINITY, INFINITY), medium.boundary_model_idx, medium.boundary_model_type, rec1))
        return false;

    if (!hit_boundary(ray, Interval(rec1.t + 0.0001, INFINITY), medium.boundary_model_idx, medium.boundary_model_type, rec2))
        return false;

    if (rec1.t < ray_t.min) rec1.t = ray_t.min;
    if (rec2.t > ray_t.max) rec2.t = ray_t.max;

    if (rec1.t >= rec2.t)
        return false;

    if (rec1.t < 0)
    rec1.t = 0;

    float ray_length = length(ray.dir);
    float distance_inside_boundary = (rec2.t - rec1.t) * ray_length;
    float hit_distance = medium.neg_inv_density * log(rand());

    if (hit_distance > distance_inside_boundary)
        return false;

    hit_record.t = rec1.t + hit_distance / ray_length;
    hit_record.p = ray.o + ray.dir * hit_record.t;
    hit_record.normal = vec3(1.0, 0.0, 0.0); // arbitrary
    hit_record.is_front_face = true; // also arbitrary

    material = medium.phase_function;
    albedo = texture_color(hit_record.p, medium.texture_id, vec2(0.0));
    color_from_emission = vec3(0.0);

    return true;
}

bool hit_model(Ray ray, Interval ray_t, int model_idx, int model_type, inout HitRecord hit_record) {
    // Check the single hit models. e.g. quads, spheres, and boxes.
    if(hit_boundary(ray, ray_t, model_idx, model_type, hit_record))
        return true;

    // If it's a more complex model like constant mediums, check using its special function.
    if(model_type == MODEL_CONSTANT_MEDIUM) {
        return hit_constant_medium(ray, ray_t, constant_mediums[model_idx], hit_record);
    }

    return false;
}