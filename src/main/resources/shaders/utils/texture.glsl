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

float trilinear_interp(float c[2][2][2], float u, float v, float w) {
    float accum = 0.0;
    for (int i=0; i < 2; i++)
        for (int j=0; j < 2; j++)
            for (int k=0; k < 2; k++)
                accum += (i * u + ( 1 - i) * (1 - u))
                * ( j * v + (1 - j) * (1 - v))
                * (k * w + (1 - k) * (1 - w))
                * c[i][j][k];

    return accum;
}

float perlin(vec3 p, int perlin_idx) {
    float u = p.x - floor(p.x);
    float v = p.y - floor(p.y);
    float w = p.z - floor(p.z);

    int i = int(floor(p.x));
    int j = int(floor(p.y));
    int k = int(floor(p.z));
    float c[2][2][2];

    PerlinNoise noise = perlin_noises[perlin_idx];

    for (int di = 0; di < 2; di++)
        for (int dj = 0; dj < 2; dj++)
            for (int dk = 0; dk < 2; dk++)
                    c[di][dj][dk] = noise.randomfloats[
                    noise.perm_x[(i + di) & 255] ^
                    noise.perm_y[(j + dj) & 255] ^
                    noise.perm_z[(k + dk) & 255]
                ];

    return trilinear_interp(c, u, v, w);
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
        case TEXTURE_PERLIN: return vec3(perlin(p, index));
        default: return vec3(0.0, 0.0, 0.0);
    }
}