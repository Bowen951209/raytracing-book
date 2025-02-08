// This file should be included after utils/random.glsl, utils/math.glsl & utils/hitting.glsl

float sphere_pdf_value() {
    return 1.0 / (4.0 * PI);
}

vec3 sphere_generate_direction() {
    return rand_unit_vec();
}

float sphere_model_pdf(vec3 origin, vec3 direction, Sphere sphere) {
    // This method only works for stationary spheres.
    HitRecord hit_record;

    if (!hit_sphere(Ray(origin, direction), Interval(0.001, INFINITY), sphere, hit_record))
        return 0.0;

    vec3 pc = sphere.center1 - origin;
    float distance_squared = dot(pc, pc);
    float cos_theta_max = sqrt(1.0 - sphere.radius * sphere.radius / distance_squared);
    float solid_angle = 2.0 * PI * (1.0 - cos_theta_max);

    return 1.0 / solid_angle;
}

vec3 sphere_model_random(vec3 origin, vec3 sphere_center, float sphere_radius) {
    vec3 direction = sphere_center - origin;
    float distance_squared = dot(direction, direction);
    return transform_onb(rand_to_sphere(sphere_radius, distance_squared), direction);
}

float cosine_pdf_value(vec3 direction, vec3 normal) {
    float cos_theta = normalize(dot(direction, normal));
    return max(0.0, cos_theta / PI);
}

vec3 cosine_generate_direction(vec3 normal) {
    return transform_onb(rand_cosine_direction(), normal);
}

float quad_pdf_value(vec3 origin, vec3 direction, Quad quad) {
    HitRecord hit_record;

    if (!hit_quad(Ray(origin, direction), Interval(0.001, INFINITY), quad, hit_record))
        return 0.0;

    float distance_squared = hit_record.t * hit_record.t * dot(direction, direction);
    float cosine = abs(dot(direction, hit_record.normal) / length(direction));

    return distance_squared / (cosine * quad.area);
}

vec3 quad_random(vec3 origin, Quad quad) {
    vec3 p = quad.q + (rand() * quad.u) + (rand() * quad.v);
    return p - origin;
}

float lights_pdf_value(vec3 origin, vec3 direction) {
    float weight = 1.0 / lights_count;
    float sum = 0.0;

    for (int i = 0; i < lights_count; i++) {
        // Get hittable_type and hittable_idx from the hittable packed value.
        int hittable_type = (lights[i] >> 16) & 0xFFFF;
        int hittable_idx = lights[i] & 0xFFFF;

        float pdf_value;
        switch(hittable_type) {
            case MODEL_SPHERE:
                pdf_value = sphere_model_pdf(origin, direction, spheres[hittable_idx]);
                break;
            case MODEL_QUAD:
                pdf_value = quad_pdf_value(origin, direction, quads[hittable_idx]);
                break;
        }

        sum += weight * pdf_value;
    }

    return sum;
}

vec3 lights_random(vec3 origin) {
    int hittable = lights[rand_int(0, lights_count - 1)];

    // Get hittable_type and hittable_idx from the hittable packed value.
    int hittable_type = (hittable >> 16) & 0xFFFF;
    int hittable_idx = hittable & 0xFFFF;

    switch(hittable_type) {
        case MODEL_SPHERE:
            return sphere_model_random(origin, spheres[hittable_idx].center1, spheres[hittable_idx].radius);
        case MODEL_QUAD:
            return quad_random(origin, quads[hittable_idx]);
    }
}

float material_pdf_value(vec3 direction, int material_val, vec3 normal) {
    int material_id = (material_val >> 16) & 0xFFFF;

    switch (material_id) {
        case MATERIAL_LAMBERTIAN:
            return cosine_pdf_value(direction, normal);
        case MATERIAL_ISOTROPIC:
            return sphere_pdf_value();
        default:
            return 0.0;
    }
}

float scattering_pdf(vec3 normal, vec3 scatter_dir, int material_val) {
    int material_id = (material_val >> 16) & 0xFFFF;

    switch (material_id) {
        case MATERIAL_LAMBERTIAN:{
            float cos_theta = dot(normal, normalize(scatter_dir));
            return max(0.0, cos_theta / PI);
        }
        case MATERIAL_ISOTROPIC:
            return 1.0 / (4.0 * PI);
    }

    return 0.0;
}