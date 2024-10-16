bool is_checker(int tex_idx) {
    ivec2 tex_size = textureSize(textures[tex_idx], 0);
    return tex_size.x == 2 && tex_size.y == 1;
}

vec3 checkerboard(vec3 p, float scale, int tex_idx) {
    float inv_scale = 1.0 / scale;

    ivec3 ip = ivec3(p * inv_scale);
    bool is_even = (ip.x + ip.y + ip.z) % 2 == 0;

    if (is_even)
        return texture2D(textures[tex_idx], vec2(0, 0)).rgb;
    else
        return texture2D(textures[tex_idx], vec2(1, 0)).rgb;
}

vec3 texture_color(vec3 p, float id) {
    int tex_idx = int(id);
    if(is_checker(tex_idx)) {
        // Scale is stored at float digits.
        float scale = id - int(id);
        return checkerboard(p, scale, tex_idx);
    }
}