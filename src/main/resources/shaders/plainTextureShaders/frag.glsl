#version 430 core


in vec2 varying_tex_coord;

out vec4 frag_color;

uniform sampler2D tex_sampler;

void main() {
    frag_color = texture(tex_sampler, varying_tex_coord);
}
