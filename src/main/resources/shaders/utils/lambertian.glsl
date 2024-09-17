// should be included after utils/random.glsl

vec3 lambertian_scatter(vec3 normal) {
    return normal + rand_unit_vec();
}