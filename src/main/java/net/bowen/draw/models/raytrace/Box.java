package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class Box extends RaytraceModel {
    private final Vector3f translation;
    private final Vector3f rotation;
    private final Material material;
    private final Quad[] sides = new Quad[6];

    /**
     * Construct the 3D box (six sides) that contains the two opposite vertices a & b, and apply the transformation
     * passed in.
     */
    public Box(Vector3f a, Vector3f b, Vector3f translation, Vector3f rotation, Material material) {
        this.material = material;
        this.translation = translation;
        this.rotation = rotation;

        // Instantiate 4d vectors in convenience to apply transformation.
        Vector3f min = new Vector3f(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
        Vector3f max = new Vector3f(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));

        Vector3f dx = new Vector3f(max.x - min.x, 0, 0);
        Vector3f dy = new Vector3f(0, max.y - min.y, 0);
        Vector3f dz = new Vector3f(0, 0, max.z - min.z);

        sides[0] = (getSide(new Vector3f(min.x, min.y, max.z), dx, dy)); // front
        sides[1] = (getSide(new Vector3f(max.x, min.y, max.z), new Vector3f(dz).negate(), dy)); // right
        sides[2] = (getSide(new Vector3f(max.x, min.y, min.z), new Vector3f(dx).negate(), dy)); // back
        sides[3] = (getSide(new Vector3f(min.x, min.y, min.z), dz, dy)); // left
        sides[4] = (getSide(new Vector3f(min.x, max.y, max.z), dx, new Vector3f(dz).negate())); // top
        sides[5] = (getSide(new Vector3f(min.x, min.y, min.z), dx, dz)); // bottom

        setBoundingBox();
    }

    /**
     * Construct the 3D box (six sides) that contains the two opposite vertices a & b, and apply no transformation.
     */
    public Box(Vector3f a, Vector3f b, Material material) {
        this(a, b, null, null, material);
    }

    private void setBoundingBox() {
        bbox = new AABB();
        for (int i = 0; i < 6; i++) {
            bbox.set(sides[i++].bbox, sides[i].boundingBox());
        }
    }

    public void putToBuffer(ByteBuffer buffer) {
        for (Quad side : sides)
            side.putToBuffer(buffer);
    }

    private Quad getSide(Vector3f q, Vector3f u, Vector3f v) {
        Vector3f tq = new Vector3f(q);
        Vector3f tu = new Vector3f(u);
        Vector3f tv = new Vector3f(v);

        if (rotation != null && translation != null) {
            Matrix3f rotationMatrix = new Matrix3f().rotateX(rotation.x).rotateY(rotation.y).rotateZ(rotation.z);
            tq.mul(rotationMatrix).add(translation);
            tu.mul(rotationMatrix);
            tv.mul(rotationMatrix);
        }

        return new Quad(tq, tu, tv, material);
    }

    @Override
    protected int getModelId() {
        return BOX_ID;
    }
}
