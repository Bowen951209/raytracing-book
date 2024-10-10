package net.bowen.draw;

import net.bowen.draw.material.Material;
import org.joml.Vector3f;

public class Sphere extends RaytraceModel {
    public static final float MODEL_ID = 0.1f;

    private final Vector3f center1 = new Vector3f(), center2 = new Vector3f();
    private final float radius;

    public Sphere(Vector3f center, float radius, Material material) {
        this(center, center, radius, material);
    }

    public Sphere(float x, float y, float z, float radius, Material material) {
        this(new Vector3f(x, y, z), radius, material);
    }

    public Sphere(Vector3f center1, Vector3f center2, float radius, Material material) {
        super(material);
        this.center1.set(center1);
        this.center2.set(center2);
        this.radius = radius;

        Vector3f vec12 = new Vector3f(center2).sub(center1);
        data = new float[]{center1.x, center1.y, center1.z, vec12.x, vec12.y, vec12.z, radius};
    }

    @Override
    protected AABB boundingBox() {
        Vector3f rvec = new Vector3f(radius);
        AABB box1 = new AABB(new Vector3f(center1).sub(rvec), new Vector3f(center1).add(rvec));
        AABB box2 = new AABB(new Vector3f(center2).sub(rvec), new Vector3f(center2).add(rvec));
        return new AABB(box1, box2);
    }
}
