#version 430 core


in vec2 varying_tex_coord;

out vec4 frag_color;

uniform sampler2D tex_sampler;

void main() {
    vec4 texture_color = texture(tex_sampler, varying_tex_coord);

    // Gamma correction
    frag_color = pow(texture_color, vec4(1.0/2.2));
}
