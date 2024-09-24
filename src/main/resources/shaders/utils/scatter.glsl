// should be included after utils/random.glsl

vec3 lambertian_scatter(vec3 normal) {
    return normal + rand_unit_vec();
}

vec3 metal_scatter(vec3 ray_in_dir, vec3 normal, float fuzz) {
    vec3 reflected = reflect(ray_in_dir, normal);
    reflected = normalize(reflected) + fuzz * rand_unit_vec();
    return reflected;
}

vec3 refract_scatter(vec3 ray_in_dir, vec3 normal, float eta) {
    vec3 unit_direction = normalize(ray_in_dir);
    float cos_theta = min(dot(-unit_direction, normal), 1.0);
    float sin_theta = sqrt(1.0 - cos_theta*cos_theta);

    // Check if total internal reflection happens.
    bool cannot_refract = eta * sin_theta > 1.0;
    vec3 direction;

    if (cannot_refract) {
        direction = reflect(unit_direction, normal);
    } else {
        direction = refract(unit_direction, normal, eta);
    }

    return direction;
}