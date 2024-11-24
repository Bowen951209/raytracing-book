package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;
import net.bowen.system.DataUtils;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Quad extends RaytraceModel {
    public static final int MODEL_ID = 2;

    private final Vector3f q, u, v, normal;
    private final float d;
    private final Material material;

    /**
     * Construct a quad for compute shader. The size and shape are determined by the u and v. The position/origin is
     * determined by the q.
     *
     * @param q        The origin for u and v.
     * @param u        A component vector that structs the quad.
     * @param v        A component vector that structs the quad.
     * @param material The material of the quad.
     */
    public Quad(Vector3f q, Vector3f u, Vector3f v, Material material) {
        this.q = q;
        this.u = u;
        this.v = v;
        this.material = material;
        this.normal = new Vector3f(u).cross(v).normalize();
        this.d = new Vector3f(normal).dot(q);

        setBoundingBox();
    }

    /**
     * @return the 3D box (six sides) that contains the two opposite vertices a & b.
     */
    public static List<Quad> getBox(Vector3f a, Vector3f b, Material material) {
        List<Quad> sides = new ArrayList<>(6);
        // Construct the two opposite vertices with the minimum and maximum coordinates.
        Vector3f min = new Vector3f(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
        Vector3f max = new Vector3f(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));

        Vector3f dx = new Vector3f(max.x - min.x, 0, 0);
        Vector3f dy = new Vector3f(0, max.y - min.y, 0);
        Vector3f dz = new Vector3f(0, 0, max.z - min.z);

        sides.add(new Quad(new Vector3f(min.x, min.y, max.z), dx, dy, material)); // front
        sides.add(new Quad(new Vector3f(max.x, min.y, max.z), new Vector3f(dz).negate(), dy, material)); // right
        sides.add(new Quad(new Vector3f(max.x, min.y, min.z), new Vector3f(dx).negate(), dy, material)); // back
        sides.add(new Quad(new Vector3f(min.x, min.y, min.z), dz, dy, material)); // left
        sides.add(new Quad(new Vector3f(min.x, max.y, max.z), dx, dz.negate(), material)); // top
        sides.add(new Quad(new Vector3f(min.x, min.y, min.z), dx, dz, material)); // bottom

        return sides;
    }

    private void setBoundingBox() {
        // Compute the bounding box of all four vertices.
        AABB bboxDiagonal1 = new AABB(q, new Vector3f(q).add(u).add(v));
        AABB bboxDiagonal2 = new AABB(new Vector3f(q).add(u), new Vector3f(q).add(v));
        bbox = new AABB(bboxDiagonal1, bboxDiagonal2);
    }

    protected void putToBuffer(ByteBuffer buffer) {
        DataUtils.putToBuffer(normal, buffer);
        buffer.putFloat(d);
        DataUtils.putToBuffer(q, buffer);
        buffer.putInt(material.getMaterialPackedValue());
        DataUtils.putToBuffer(u, buffer);
        buffer.putInt(material.getTexturePackedValue());
        DataUtils.putToBuffer(v, buffer);
        buffer.putFloat(0); // padding
        DataUtils.putToBuffer(material.emitted(), buffer);
        buffer.putFloat(0); // padding
    }

    @Override
    protected int getModelId() {
        return MODEL_ID;
    }
}
