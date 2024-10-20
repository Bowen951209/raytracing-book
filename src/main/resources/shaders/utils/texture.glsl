vec3 checkerboard(vec3 p, float scale, int tex_idx) {
    float inv_scale = 1.0 / scale;

    ivec3 ip = ivec3(p * inv_scale);
    bool is_even = (ip.x + ip.y + ip.z) % 2 == 0;

    if (is_even)
        return texture2D(textures[tex_idx], vec2(0, 0)).rgb;
    else
        return texture2D(textures[tex_idx], vec2(1, 0)).rgb;
}

vec2 get_sphere_uv(vec3 p) {
    // p: a given point on the sphere of radius one, centered at the origin.
    // u: returned value [0,1] of angle around the Y axis from X=-1.
    // v: returned value [0,1] of angle from Y=-1 to Y=+1.
    //     <1 0 0> yields <0.50 0.50>       <-1  0  0> yields <0.00 0.50>
    //     <0 1 0> yields <0.50 1.00>       < 0 -1  0> yields <0.50 0.00>
    //     <0 0 1> yields <0.25 0.50>       < 0  0 -1> yields <0.75 0.50>

    // It is better to use the outward normal of a sphere, but for designing difficulty, I just normalize the p here.
    p = normalize(p);
    float theta = acos(-p.y);
    float phi = atan(-p.z, p.x) + PI;

    return vec2(phi / (2.0 * PI), theta / PI);
}

vec3 texture_color(vec3 p, int id) {
    // Extract texture index in upper 16 bits.
    int tex_idx = (id >> 16) & 0xFFFF;

    // Extract detail value from the lower 16 bits
    int detail_quantized = id & 0xFFFF;

    // Convert detail value back to float (undo the quantization)
    float detail_val = float(detail_quantized) / 65535.0;

    if(detail_val >= 0.001) {
        // If there are float digits, the texture is a checkerboard.

        // Scale is equal to the detail_val.
        return checkerboard(p, detail_val, tex_idx);
    } else {
        // It's an image texture.
        return texture2D(textures[tex_idx], get_sphere_uv(p)).rgb;
    }
}