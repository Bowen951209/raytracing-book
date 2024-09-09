#version 430 core

layout(location = 0) in vec3 vertex_position;
layout(location = 1) in vec2 texture_coord;


out vec2 varying_tex_coord;

void main() {
    varying_tex_coord = texture_coord;
    gl_Position = vec4(vertex_position, 1.0);
}
