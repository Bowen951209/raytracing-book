package net.bowen.draw;

public class Sphere extends RaytraceModel {
    public Sphere(float x, float y, float z, float radius, int material, float albedoR, float albedoG, float albedoB) {
        data = new float[] {x, y, z, radius, material, albedoR, albedoG, albedoB};
    }
}
