vec3 checkerboard(vec3 p) {
    // (for hard-coded properties now.)
    float inv_scale = 1.0 / 0.32;

    ivec3 ip = ivec3(p * inv_scale);
    bool is_even = (ip.x + ip.y + ip.z) % 2 == 0;

    if (is_even)
        return vec3(.2, .3, .1);
    else
        return vec3(.9, .9, .9);
}