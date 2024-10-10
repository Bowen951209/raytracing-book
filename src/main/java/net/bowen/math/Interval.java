package net.bowen.math;

public class Interval {
    public float min;
    public float max;

    public Interval() {
        set(-Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public Interval set(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public Interval set(Interval interval) {
        return set(interval.min, interval.max);
    }

    public Interval set(Interval a, Interval b) {
        return set(Math.min(a.min, b.min),  Math.max(a.max, b.max));
    }
}
