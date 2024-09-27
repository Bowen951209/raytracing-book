// Return a random.glsl value in section [0, 1).
float rand() {
    rand_factor += 0.001;
    vec2 co = pixel_coord;
    co += rand_factor;
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// Return a random.glsl value in section [minVal, maxVal).
float rand(float minVal, float maxVal) {
    return minVal + rand() * (maxVal - minVal);
}

vec3 rand_vec3(float min_val, float max_val) {
    float rand1 = rand(min_val, max_val);
    float rand2 = rand(min_val, max_val);
    float rand3 = rand(min_val, max_val);
    return vec3(rand1, rand2, rand3);
}

vec3 rand_vec_in_unit_sphere() {
    while (true) {
        vec3 p = rand_vec3(-1.0, 1.0);
        if (dot(p, p) < 1) return p;
    }
}

vec3 rand_unit_vec() {
    return normalize(rand_vec_in_unit_sphere());
}

vec3 rand_on_hemisphere(vec3 normal) {
    vec3 on_unit_sphere = rand_unit_vec();
    if(dot(on_unit_sphere, normal) > 0.0) { // In the same hemisphere as the normal
        return on_unit_sphere;
    }
    return -on_unit_sphere;
}

vec3 pixel_sample_square() {
    float px = -0.5 + rand();
    float py = -0.5 + rand();
    return px * pixel_delta_u + py * pixel_delta_v;
}