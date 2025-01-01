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

vec3 random_in_unit_disk() {
    while (true) {
        vec3 p = vec3(rand(-1.0, 1.0), rand(-1.0, 1.0), 0.0);
        if (dot(p, p) < 1) return p;
    }
}

// Returns a random point in the camera defocus disk.
vec3 defocus_disk_sample() {
    vec3 p = random_in_unit_disk();
    return camera_pos + (p.x * defocus_disk_u) + (p.y * defocus_disk_v);
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
    float sqrt_frame_count = mod(float(frame_count), sqrt_spp); // Column index in the stratified grid
    float layer = float(frame_count) / sqrt_spp;                // Row index in the stratified grid

    // Calculate the center point of the current grid cell
    float base_x = (sqrt_frame_count + 0.5) * recip_sqrt_spp;
    float base_y = (layer + 0.5) * recip_sqrt_spp;

    // Add random jitter within the grid cell
    float jitter_x = (rand() - 0.5) * recip_sqrt_spp;
    float jitter_y = (rand() - 0.5) * recip_sqrt_spp;

    // Adjust the sample position to the local pixel space
    float px = base_x + jitter_x - 0.5;
    float py = base_y + jitter_y - 0.5;

    // Return the sampling position vector
    return px * pixel_delta_u + py * pixel_delta_v;
}