package net.bowen.draw;

import net.bowen.draw.material.Material;
import org.joml.Vector3f;

public class Sphere extends RaytraceModel {
    public Sphere(Vector3f center, float radius, Material material) {
        this(center.x, center.y, center.z, radius, material);
    }

    public Sphere(float x, float y, float z, float radius, Material material) {
        this(x, y, z, x, y, z, radius, material);
    }

    public Sphere(Vector3f center1, Vector3f center2, float radius, Material material) {
        this(center1.x, center1.y, center1.z, center2.x, center2.y, center2.z, radius, material);
    }

    public Sphere(float x1, float y1, float z1, float x2, float y2, float z2, float radius, Material material) {
        super(material);
        data = new float[] {x1, y1, z1, x2 - x1, y2 - y1, z2 - z1, radius};
    }
}
