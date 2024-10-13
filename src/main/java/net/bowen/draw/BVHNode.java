package net.bowen.draw;

import net.bowen.math.Interval;

import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.List;

public class BVHNode extends RaytraceModel {
    public static final float MODEL_ID = 0f;

    public final RaytraceModel left, right;

    public BVHNode(List<? extends RaytraceModel> objects, int start, int end) {
        super(null);
        BVH_NODES.add(this);
        id = BVH_NODES.size() - 1 + MODEL_ID;

        bbox = new AABB().empty();
        for (int i = start; i < end; i++)
            bbox.set(bbox, objects.get(i).boundingBox());

        int axis = bbox.longestAxis();

        Comparator<RaytraceModel> comparator = (axis == 0) ? BVHNode::boxXCompare
                : (axis == 1) ? BVHNode::boxYCompare
                : BVHNode::boxZCompare;

        int objectSpan = end - start;

        if (objectSpan == 1) {
            left = right = objects.get(start);
        } else if (objectSpan == 2) {
            left = objects.get(start);
            right = objects.get(start + 1);
        } else {
            objects.subList(start, end).sort(comparator);

            int mid = start + objectSpan / 2;
            left = new BVHNode(objects, start, mid);
            right = new BVHNode(objects, mid, end);
        }
    }

    public void putToBuffer(FloatBuffer buffer) {
        buffer.put(bbox.x.min).put(bbox.x.max);
        buffer.put(bbox.y.min).put(bbox.y.max);
        buffer.put(bbox.z.min).put(bbox.z.max);
        buffer.put(left.id);
        buffer.put(right.id);
    }

    private static int boxCompare(RaytraceModel a, RaytraceModel b, int axis) {
        Interval aAxisInterval = a.boundingBox().axisInterval(axis);
        Interval bAxisInterval = b.boundingBox().axisInterval(axis);
        if (aAxisInterval.min < bAxisInterval.min)
            return 1;
        return -1;
    }

    private static int boxXCompare(RaytraceModel a, RaytraceModel b) {
        return boxCompare(a, b, 0);
    }

    private static int boxYCompare(RaytraceModel a, RaytraceModel b) {
        return boxCompare(a, b, 1);
    }

    private static int boxZCompare(RaytraceModel a, RaytraceModel b) {
        return boxCompare(a, b, 2);
    }
}
