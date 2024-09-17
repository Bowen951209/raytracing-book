package net.bowen.draw;

public class Sphere extends RaytraceModel {
    public Sphere(float x, float y, float z, float radius) {
        data = new float[] {x, y, z, radius};
    }
}
