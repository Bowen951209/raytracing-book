const float PI = 3.14159265359;

vec3 transform_onb(vec3 vec, vec3 normal, out vec3 w) {
    vec3 u, v;
    w = normalize(normal);
    vec3 a = (abs(w.x) > 0.9) ? vec3(0, 1, 0) : vec3(1, 0, 0);
    v = normalize(cross(w, a));
    u = cross(w, v);

    mat3 transform = mat3(u, v, w);
    return transform * vec;
}