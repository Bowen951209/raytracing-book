// should be included after utils/random.glsl

vec3 lambertian_scatter(vec3 normal) {
    return normal + rand_unit_vec();
}

vec3 metal_scatter(vec3 ray_in_dir, vec3 normal, float fuzz) {
    return reflect(ray_in_dir, normal + fuzz * rand_unit_vec());
}