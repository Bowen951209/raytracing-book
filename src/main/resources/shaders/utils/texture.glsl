const int TEXTURE_IMAGE = 1;
const int TEXTURE_CHECKER = 2;
const int TEXTURE_PERLIN = 3;


vec3 checkerboard(vec3 p, float scale, int tex_idx) {
    float inv_scale = 1.0 / scale;

    ivec3 ip = ivec3(p * inv_scale);
    bool is_even = (ip.x + ip.y + ip.z) % 2 == 0;

    if (is_even)
        return texture2D(textures[tex_idx], vec2(0, 0)).rgb;
    else
        return texture2D(textures[tex_idx], vec2(1, 0)).rgb;
}

float perlin_interp(vec3 c[2][2][2], float u, float v, float w) {
    float uu = u * u * (3.0 - 2.0 * u);
    float vv = v * v * (3.0 - 2.0 * v);
    float ww = w * w * (3.0 - 2.0 * w);
    float accum = 0.0;

    for (int i = 0; i < 2; i++)
        for (int j = 0; j < 2; j++)
            for (int k = 0; k < 2; k++) {
                vec3 weight_v = vec3(u - float(i), v - float(j), w - float(k));
                accum += (i * uu + (1.0 - float(i)) * (1.0 - uu))
                * (j * vv + (1.0 - float(j)) * (1.0 - vv))
                * (k * ww + (1.0 - float(k)) * (1.0 - ww))
                * dot(c[i][j][k], weight_v);
            }

    return accum;
}

float perlin(vec3 p, int tex_idx) {
    float u = p.x - floor(p.x);
    float v = p.y - floor(p.y);
    float w = p.z - floor(p.z);
    u = u * u * (3 - 2 * u);
    v = v * v * (3 - 2 * v);
    w = w * w * (3 - 2 * w);

    int i = int(floor(p.x));
    int j = int(floor(p.y));
    int k = int(floor(p.z));
    vec3 c[2][2][2];

    int perm_x, perm_y, perm_z, rand_vec_idx;

    for (int di = 0; di < 2; di++) {
        for (int dj = 0; dj < 2; dj++) {
            for (int dk = 0; dk < 2; dk++) {
                //  - random vectors' xyz are in row 0, 1, and 2.
                //  - perm_x values are in row 3.
                //  - perm_y values are in row 4.
                //  - perm_z values are in row 5.

                perm_x = int(texelFetch(textures[tex_idx], ivec2(3, (i + di) & 255), 0).r);
                perm_y = int(texelFetch(textures[tex_idx], ivec2(4, (j + dj) & 255), 0).r);
                perm_z = int(texelFetch(textures[tex_idx], ivec2(5, (k + dk) & 255), 0).r);

                rand_vec_idx = perm_x ^ perm_y ^ perm_z;

                c[di][dj][dk] = vec3(
                    texelFetch(textures[tex_idx], ivec2(0, rand_vec_idx), 0).r,
                    texelFetch(textures[tex_idx], ivec2(1, rand_vec_idx), 0).r,
                    texelFetch(textures[tex_idx], ivec2(2, rand_vec_idx), 0).r
                );
            }
        }
    }

    // The return value of perlin_interp() is in range [-1, 1], we're going to scale it to [0, 1].
    float interp = perlin_interp(c, u, v, w);
    return 0.5 * (1.0 + interp);
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
    // Extract detail from the lower 12 bits (bits 0 to 11)
    int detail_bits = id & 0xFFF;

    // Extract index from the middle 16 bits (bits 12 to 27)
    int index = (id >> 12) & 0xFFFF;

    // Extract textureTypeId from the upper 4 bits (bits 28 to 31)
    int texture_type = (id >> 28) & 0xF;

    // Convert the 12-bit detail integer back to a float (range [0, 1])
    float detail = float(detail_bits) / 4095.0;


    switch(texture_type) {
        case TEXTURE_CHECKER: return checkerboard(p, detail, index);
        case TEXTURE_IMAGE: return texture2D(textures[index], get_sphere_uv(p)).rgb;
        case TEXTURE_PERLIN: return vec3(perlin(detail * 100.0 * p, index)); // detail x 100 is the scale.
        default: return vec3(0.0, 0.0, 0.0);
    }
}