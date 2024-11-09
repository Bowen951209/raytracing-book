package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;
import net.bowen.system.DataUtils;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class Sphere extends RaytraceModel {
    public static final int MODEL_ID = 1;

    private final Vector3f center1, vec12;
    private final float radius;

    public Sphere(Vector3f center, float radius, Material material) {
        this(center, center, radius, material);
    }

    public Sphere(float x, float y, float z, float radius, Material material) {
        this(new Vector3f(x, y, z), radius, material);
    }

    public Sphere(Vector3f center1, Vector3f center2, float radius, Material material) {
        super(material);
        this.center1 = center1;
        this.vec12 = new Vector3f(center2).sub(center1);
        this.radius = radius;

        Vector3f rvec = new Vector3f(radius);
        AABB box1 = new AABB(new Vector3f(center1).sub(rvec), new Vector3f(center1).add(rvec));
        AABB box2 = new AABB(new Vector3f(center2).sub(rvec), new Vector3f(center2).add(rvec));
        bbox = new AABB(box1, box2);
    }

    protected void putToBuffer(ByteBuffer buffer) {
        // Center position (vec3)
        DataUtils.putToBuffer(center1, buffer);

        // Material id (int).
        buffer.putInt(material.getTexturePackedValue());

        // Center vector (vec3)
        DataUtils.putToBuffer(vec12, buffer);

        // Radius (float)
        buffer.putFloat(radius);

        // Material (int)
        buffer.putInt(material.getMaterialPackedValue());

        // Paddings.
        buffer.putInt(0).putInt(0).putInt(0);
    }

    @Override
    protected int getModelId() {
        return MODEL_ID;
    }
}
