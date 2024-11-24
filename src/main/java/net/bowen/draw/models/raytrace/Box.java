package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.ArrayList;

public class Box extends ArrayList<RaytraceModel> {
    private final Vector3f translation;
    private final Vector3f rotation;
    private final Material material;

    /**
     * Construct the 3D box (six sides) that contains the two opposite vertices a & b, and apply the transformation
     * passed in.
     */
    public Box(Vector3f a, Vector3f b, Vector3f translation, Vector3f rotation, Material material) {
        super(6); // a box has six sides
        this.translation = translation;
        this.rotation = rotation;
        this.material = material;

        // Instantiate 4d vectors in convenience to apply transformation.
        Vector3f min = new Vector3f(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
        Vector3f max = new Vector3f(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));

        Vector3f dx = new Vector3f(max.x - min.x, 0, 0);
        Vector3f dy = new Vector3f(0, max.y - min.y, 0);
        Vector3f dz = new Vector3f(0, 0, max.z - min.z);

        add(getSide(new Vector3f(min.x, min.y, max.z), dx, dy)); // front
        add(getSide(new Vector3f(max.x, min.y, max.z), new Vector3f(dz).negate(), dy)); // right
        add(getSide(new Vector3f(max.x, min.y, min.z), new Vector3f(dx).negate(), dy)); // back
        add(getSide(new Vector3f(min.x, min.y, min.z), dz, dy)); // left
        add(getSide(new Vector3f(min.x, max.y, max.z), dx, new Vector3f(dz).negate())); // top
        add(getSide(new Vector3f(min.x, min.y, min.z), dx, dz)); // bottom
    }

    private Quad getSide(Vector3f q, Vector3f u, Vector3f v) {
        Matrix3f rotationMatrix = new Matrix3f().rotateX(rotation.x).rotateY(rotation.y).rotateZ(rotation.z);

        Vector3f tq = new Vector3f(q).mul(rotationMatrix).add(translation);
        Vector3f tu = new Vector3f(u).mul(rotationMatrix);
        Vector3f tv = new Vector3f(v).mul(rotationMatrix);

        return new Quad(tq, tu, tv, material);
    }
}
