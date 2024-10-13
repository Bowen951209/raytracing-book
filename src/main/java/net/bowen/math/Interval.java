package net.bowen.math;

public class Interval {
    public float min;
    public float max;

    /**
     * Construct an empty interval of [INFINITY, -INFINITY], where INFINITY is the max of float.
     */
    public Interval() {
        set(Float.MAX_VALUE, -Float.MAX_VALUE);
    }

    public Interval set(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public Interval set(Interval interval) {
        return set(interval.min, interval.max);
    }

    /**
     * Construct an interval that is the union of the given intervals.
     */
    public Interval set(Interval a, Interval b) {
        return set(Math.min(a.min, b.min),  Math.max(a.max, b.max));
    }

    /**
     * @return the size of the interval, which is max - min.
     */
    public float size() {
        return max - min;
    }
}
