// should be included after utils/random.glsl

vec3 lambertian_scatter(vec3 normal) {
    return normal + rand_unit_vec();
}

void metal_scatter(inout vec3 ray_dir, vec3 normal, float fuzz) {
    ray_dir = reflect(ray_dir, normal);
    ray_dir = normalize(ray_dir) + fuzz * rand_unit_vec();
}

float reflectance(float cos_theta, float eta) {
    // Use Schlick's approximation for reflectance.
    float r0 = (1 - eta) / (1 + eta);
    r0 = r0*r0;
    return r0 + (1 - r0) * pow((1 - cos_theta), 5);
}

void refract_scatter(inout vec3 ray_dir, vec3 normal, float eta) {
    ray_dir = normalize(ray_dir);
    float cos_theta = min(dot(-ray_dir, normal), 1.0);
    float sin_theta = sqrt(1.0 - cos_theta*cos_theta);

    // Check if total internal reflection happens.
    bool cannot_refract = eta * sin_theta > 1.0;

    if (cannot_refract || reflectance(cos_theta, eta) > rand()) {
        ray_dir = reflect(ray_dir, normal);
    } else {
        ray_dir = refract(ray_dir, normal, eta);
    }
}