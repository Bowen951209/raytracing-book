#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;


layout (local_size_x = 16, local_size_y = 16) in;

struct Ray {
    vec3 o;     // origin
    vec3 dir;   // direction
};

bool hit_sphere(vec3 center, float radius, Ray ray) {
    vec3 oc = ray.o - center;
    float a = dot(ray.dir, ray.dir);
    float b = 2.0 * dot(oc, ray.dir);
    float c = dot(oc, oc) - radius * radius;
    float discriminant = b * b - 4 * a * c;
    return discriminant >= 0;
}

vec2 get_norm_coord(vec2 pixel_coord) {
    vec2 img_size = vec2(imageSize(img_output));
    float aspect_ratio = img_size.x / img_size.y;

    // Get the normalized coordinate. For the case of aspect_ratio >= 1, y should be transformed into range [-1, 1]
    //, and x should have the same scale as y (translation should be scaled too).
    vec2 coord;
    coord.x = pixel_coord.x / img_size.y * 2.0 - aspect_ratio;
    coord.y = pixel_coord.y / img_size.y * 2.0 - 1.0;
    return coord;
}

Ray get_ray(vec2 normal_coord) {
    Ray ray;
    ray.o = vec3(0.0);
    ray.dir = vec3(normal_coord, -1.0);

    return ray;
}

void main() {
    ivec2 pixel_coord = ivec2(gl_GlobalInvocationID.xy);
    Ray ray = get_ray(get_norm_coord(vec2(pixel_coord)));

    vec4 color = vec4(0.0, 0.0, 0.0, 1.0);
    if(hit_sphere(vec3(0.0, 0.0, -1.0), 0.5, ray)) {
        color = vec4(1.0, 1.0, 1.0, 1.0);
    }

    imageStore(img_output, pixel_coord, color);
}