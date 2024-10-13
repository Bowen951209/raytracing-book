package net.bowen.draw;

import net.bowen.math.Interval;
import org.joml.Vector3f;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class AABB extends RaytraceModel {
    public final Interval x = new Interval(), y = new Interval(), z = new Interval();

    /**
     * Construct an empty AABB with empty intervals x, y, and z.
     */
    public AABB() {
    }

    public AABB(Interval x, Interval y, Interval z) {
        this.x.set(x);
        this.y.set(y);
        this.z.set(z);
    }

    public AABB(Vector3f pointA, Vector3f pointB) {
        x.set(min(pointA.x, pointB.x), max(pointA.x, pointB.x));
        y.set(min(pointA.y, pointB.y), max(pointA.y, pointB.y));
        z.set(min(pointA.z, pointB.z), max(pointA.z, pointB.z));
    }

    public AABB(AABB box1, AABB box2) {
        set(box1, box2);
    }

    public AABB set(AABB box1, AABB box2) {
        x.set(box1.x, box2.x);
        y.set(box1.y, box2.y);
        z.set(box1.z, box2.z);
        return this;
    }

    public Interval axisInterval(int n) {
        if (n == 1) return y;
        if (n == 2) return z;
        return x;
    }

    public int longestAxis() {
        // Returns the index of the longest axis of the bounding box.
        if (x.size() > y.size())
            return x.size() > z.size() ? 0 : 2;
        else
            return y.size() > z.size() ? 1 : 2;
    }
}
