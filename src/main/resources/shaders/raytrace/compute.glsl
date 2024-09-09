#version 430 core

layout (rgba32f, binding = 0) uniform image2D img_output;


layout (local_size_x = 16, local_size_y = 16) in;

void main() {
    ivec2 pixel_coord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 image_size = imageSize(img_output);

    vec2 normalCoord = vec2(pixel_coord) / vec2(image_size);


    vec4 color = vec4(normalCoord.x, normalCoord.y, 1.0, 1.0);
    imageStore(img_output, pixel_coord, color);
}