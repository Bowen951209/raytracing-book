// This file should be included after utils/random.glsl, utils/math.glsl & utils/hitting.glsl

float sphere_pdf_value() {
    return 1.0 / (4.0 * PI);
}

vec3 sphere_generate_direction() {
    return rand_unit_vec();
}

float cosine_pdf_value(vec3 direction, vec3 w) {
    float cos_theta = dot(normalize(direction), w);
    return max(0.0, cos_theta / PI);
}

vec3 cosine_generate_direction(vec3 normal, out vec3 w) {
    return transform_onb(rand_cosine_direction(), normal, w);
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

float pdf_value(vec3 direction, int material_val, vec3 w) {
    int material_id = (material_val >> 16) & 0xFFFF;

    switch (material_id) {
        case MATERIAL_LAMBERTIAN:
            return cosine_pdf_value(direction, w);
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