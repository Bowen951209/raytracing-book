package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;
import net.bowen.system.DataUtils;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class Quad extends RaytraceModel{
    public static final int MODEL_ID = 2;

    private final Vector3f q, u, v, w, normal;
    private final float d;
    private final Material material;

    /**
     * Construct a quad for compute shader. The size and shape are determined by the u and v. The position/origin is
     * determined by the q.
     *
     * @param q The origin for u and v.
     * @param u A component vector that structs the quad.
     * @param v A component vector that structs the quad.
     * @param material The material of the quad.
     */
    public Quad(Vector3f q, Vector3f u, Vector3f v, Material material) {
        this.q = q;
        this.u = u;
        this.v = v;
        this.material = material;
        Vector3f n = new Vector3f(u).cross(v);
        this.normal = new Vector3f(n).normalize();
        this.d = new Vector3f(normal).dot(q);
        this.w = new Vector3f(n).div(n.lengthSquared());

        setBoundingBox();
    }

    private void setBoundingBox() {
        // Compute the bounding box of all four vertices.
        AABB bboxDiagonal1 = new AABB(q, new Vector3f(q).add(u).add(v));
        AABB bboxDiagonal2 = new AABB(new Vector3f(q).add(u), new Vector3f(q).add(v));
        bbox = new AABB(bboxDiagonal1, bboxDiagonal2);
    }

    @Override
    protected void putToBuffer(ByteBuffer buffer) {
        DataUtils.putToBuffer(normal, buffer);
        buffer.putFloat(d);
        DataUtils.putToBuffer(q, buffer);
        buffer.putInt(material.getValue());
        DataUtils.putToBuffer(u, buffer);
        buffer.putFloat(0); // padding
        DataUtils.putToBuffer(v, buffer);
        buffer.putFloat(0); // padding
        DataUtils.putToBuffer(w, buffer);
        buffer.putFloat(0); // padding

        float[] albedo = material.getAlbedo();
        buffer.putFloat(albedo[0]); // r
        buffer.putFloat(albedo[1]); // g
        buffer.putFloat(albedo[2]); // b
        buffer.putFloat(0); // padding
    }

    @Override
    protected int getModelId() {
        return MODEL_ID;
    }
}
