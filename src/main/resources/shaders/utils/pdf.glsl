// This file should be included after utils/random.glsl & utils/math.glsl

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