package net.bowen.draw;

import net.bowen.draw.material.Material;

public class Sphere extends RaytraceModel {
    public Sphere(float x, float y, float z, float radius, Material material) {
        super(material);
        data = new float[] {x, y, z, radius};
    }
}
