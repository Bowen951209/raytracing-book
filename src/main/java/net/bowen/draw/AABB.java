package net.bowen.draw;

import net.bowen.math.Interval;
import org.joml.Vector3f;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class AABB extends RaytraceModel {
    public final Interval x = new Interval(), y = new Interval(), z = new Interval();

    public AABB() {
        super(null);
    }

    public AABB(Interval x, Interval y, Interval z) {
        super(null);
        this.x.set(x);
        this.y.set(y);
        this.z.set(z);
    }

    public AABB(Vector3f pointA, Vector3f pointB) {
        super(null);
        x.set(min(pointA.x, pointB.x), max(pointA.x, pointB.x));
        y.set(min(pointA.y, pointB.y), max(pointA.y, pointB.y));
        z.set(min(pointA.z, pointB.z), max(pointA.z, pointB.z));
    }

    public AABB(AABB box1, AABB box2) {
        super(null);
        x.set(box1.x, box2.x);
        y.set(box1.y, box2.y);
        z.set(box1.z, box2.z);
    }

    public Interval axisInterval(int n) {
        if (n == 1) return y;
        if (n == 2) return z;
        return x;
    }

    @Override
    protected AABB boundingBox() {
        // We should not query a bounding box of a AABB.
        return null;
    }
}
