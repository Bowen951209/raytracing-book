// should be included after utils/random.glsl & utils/math.glsl

bool near_zero(vec3 v) {
    float s = 1e-8;
    return abs(v.x) < s && abs(v.y) < s && abs(v.z) < s;
}

vec3 lambertian_scatter(vec3 normal, out float pdf) {
    vec3 w;
    vec3 scatter_dir = transform_onb(rand_cosine_direction(), normal, w);
    pdf = dot(scatter_dir, w) / PI;
    return normalize(scatter_dir);
}

float lambertian_scattering_pdf(vec3 normal, vec3 scatter_dir) {
    return 1 / (2 * PI);
}

void metal_scatter(inout vec3 ray_dir, vec3 normal, float fuzz) {
    ray_dir = reflect(ray_dir, normal);
    ray_dir = normalize(ray_dir) + fuzz * rand_unit_vec();
}

float reflectance(float cos_theta, float eta) {
    // Use Schlick's approximation for reflectance.
    float r0 = (1 - eta) / (1 + eta);
    r0 = r0*r0;
    return r0 + (1 - r0) * pow((1 - cos_theta), 5);
}

void refract_scatter(inout vec3 ray_dir, vec3 normal, float eta) {
    ray_dir = normalize(ray_dir);
    float cos_theta = min(dot(-ray_dir, normal), 1.0);
    float sin_theta = sqrt(1.0 - cos_theta*cos_theta);

    // Check if total internal reflection happens.
    bool cannot_refract = eta * sin_theta > 1.0;

    if (cannot_refract || reflectance(cos_theta, eta) > rand()) {
        ray_dir = reflect(ray_dir, normal);
    } else {
        ray_dir = refract(ray_dir, normal, eta);
    }
}

void isotropic_scatter(inout Ray ray, vec3 p, out float pdf) {
    ray = Ray(p, rand_unit_vec());
    pdf = 1.0 / (4.0 * PI);
}

float isotropic_scattering_pdf() {
    return 1.0 / (4.0 * PI);
}

bool scatter(inout Ray ray, vec3 hit_point, vec3 normal, bool is_front_face, int material_val, out float pdf) {
    // Extract material ID from the upper 16 bits
    int material_id = (material_val >> 16) & 0xFFFF;
    bool should_scatter;

    switch (material_id) {
        case MATERIAL_LAMBERTIAN: {
            ray.dir = lambertian_scatter(normal, pdf);
            should_scatter = true;
            break;
        }
        case MATERIAL_METAL: {
            // Extract fuzz from the lower 16 bits
            int fuzz_quantized = material_val & 0xFFFF;

            // Convert fuzz back to float (undo the quantization)
            float fuzz = float(fuzz_quantized) / 65535.0;

            metal_scatter(ray.dir, normal, fuzz);
            should_scatter = dot(ray.dir, normal) > 0.0; // check if the ray is absorbed by the metal
            break;
        }
        case MATERIAL_DIELECTRIC: {
            // Extract quantized IOR from the lower 16 bits
            int iorQuantized = material_val & 0xFFFF;

            // Convert quantized IOR back to normalized float [0, 1]
            float normalizedIOR = float(iorQuantized) / 65535.0;

            // Scale back to the original IOR range [1.0, 2.5]
            float eta = mix(MIN_IOR, MAX_IOR, normalizedIOR);

            if (is_front_face) eta = 1.0 / eta;
            refract_scatter(ray.dir, normal, eta);
            should_scatter = true;
            break;
        }
        case MATERIAL_DIFFUSE_LIGHT:
            return false;
        case MATERIAL_ISOTROPIC: {
            isotropic_scatter(ray, hit_point, pdf);
            should_scatter = true;
            break;
        }
    }

    // Catch degenerate scatter direction.
    if (near_zero(ray.dir))
        ray.dir = normal;

    return should_scatter;
}

float scattering_pdf(vec3 normal, vec3 scatter_dir, int material_val) {
    // Extract material ID from the upper 16 bits
    int material_id = (material_val >> 16) & 0xFFFF;

    switch (material_id) {
        case MATERIAL_LAMBERTIAN:
            return lambertian_scattering_pdf(normal, scatter_dir);
        case MATERIAL_ISOTROPIC:
            return isotropic_scattering_pdf();
    }

    return 0.0;
}