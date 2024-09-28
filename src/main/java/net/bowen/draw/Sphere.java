package net.bowen.draw;

import net.bowen.draw.material.Material;
import org.joml.Vector3f;

public class Sphere extends RaytraceModel {
    public Sphere(Vector3f center, float radius, Material material) {
        this(center.x, center.y, center.z, radius, material);
    }

    public Sphere(float x, float y, float z, float radius, Material material) {
        super(material);
        data = new float[] {x, y, z, radius};
    }
}
