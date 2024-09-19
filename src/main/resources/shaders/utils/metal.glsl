vec3 metal_scatter(vec3 ray_in_dir, vec3 normal) {
    return reflect(ray_in_dir, normal);
}