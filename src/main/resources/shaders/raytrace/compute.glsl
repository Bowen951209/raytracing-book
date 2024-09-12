#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;


layout (local_size_x = 16, local_size_y = 16) in;

struct Ray {
    vec3 o;     // origin
    vec3 dir;   // direction
};

// Return the distance from the ray to the sphere.
float hit_sphere(vec3 center, float radius, Ray ray) {
    vec3 oc = ray.o - center;
    float a = dot(ray.dir, ray.dir);
    float b = 2.0 * dot(oc, ray.dir);
    float c = dot(oc, oc) - radius * radius;
    float discriminant = b * b - 4 * a * c;

    if(discriminant < 0)
        return -1.0;
    else
        return (-b - sqrt(discriminant)) / (2.0 * a);
}

vec2 get_norm_coord(vec2 pixel_coord) {
    vec2 img_size = vec2(imageSize(img_output));
    float aspect_ratio = img_size.x / img_size.y;

    // Get the normalized coordinate. For the case of aspect_ratio >= 1, y should be transformed into range [-1, 1]
    //, and x should have the same scale as y (translation should be scaled too).
    vec2 coord;
    coord.x = pixel_coord.x / img_size.y * 2.0 - aspect_ratio;
    coord.y = -(pixel_coord.y / img_size.y * 2.0 - 1.0);
    return coord;
}

Ray get_ray(vec2 normal_coord) {
    Ray ray;
    ray.o = vec3(0.0);
    ray.dir = vec3(normal_coord, -1.0);

    return ray;
}

vec3 get_color(Ray ray) {
    vec3 sphere_o = vec3(0.0, 0.0, -1.0);
    float sphere_radius = 0.5;
    float t = hit_sphere(sphere_o, sphere_radius, ray);

    if(t > 0.0) {
        vec3 n = (ray.o + ray.dir * t - sphere_o) / sphere_radius;
        return 0.5 * vec3(n.x + 1.0, n.y + 1.0, n.z + 1.0);
    }

    vec3 unit_direction = normalize(ray.dir);
    float a = 0.5 * (unit_direction.y + 1.0);
    return (1.0 - a) * vec3(1.0) + a * vec3(0.5, 0.7, 1.0);
}

void main() {
    ivec2 pixel_coord = ivec2(gl_GlobalInvocationID.xy);
    Ray ray = get_ray(get_norm_coord(vec2(pixel_coord)));
    imageStore(img_output, pixel_coord, vec4(get_color(ray), 1.0));
}