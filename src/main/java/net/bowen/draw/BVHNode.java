package net.bowen.draw;

import net.bowen.math.Interval;

import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.List;

public class BVHNode extends RaytraceModel {
    public static final float MODEL_ID = 0f;

    // Left and right children.
    public final RaytraceModel left, right;

    public BVHNode(List<? extends RaytraceModel> objects, int start, int end) {
        // Add self to the static BVH nodes list.
        BVH_NODES.add(this);

        // Give this instance an id, which can be calculated using the size of BVH_NODES. Also, we want to mark that
        // this is a BVH node object, not a drawable, so we add the BVH model id.
        id = BVH_NODES.size() - 1 + MODEL_ID;

        // Build the bounding box of the span of source objects.
        bbox = new AABB();
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
