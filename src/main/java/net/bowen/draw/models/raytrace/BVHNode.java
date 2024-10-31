package net.bowen.draw.models.raytrace;

import net.bowen.math.Interval;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

public class BVHNode extends RaytraceModel {
    public static final int MODEL_ID = 0;

    // Left and right children.
    public final RaytraceModel left, right;

    public BVHNode(List<? extends RaytraceModel> objects, int start, int end) {
        // Add self to the static BVH nodes list.
        BVH_NODES.add(this);

        // The index in list.
        indexInList = BVH_NODES.size() - 1;

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

    @Override
    protected void putToBuffer(ByteBuffer buffer) {
        buffer.putFloat(bbox.x.min).putFloat(bbox.x.max);
        buffer.putFloat(bbox.y.min).putFloat(bbox.y.max);
        buffer.putFloat(bbox.z.min).putFloat(bbox.z.max);


        // Put the index in list in upper 16 bits; model id in lower 16 bits.
        buffer.putInt((left.indexInList << 16) | (left.getModelId() & 0xFFFF));
        buffer.putInt((right.indexInList << 16) | (right.getModelId() & 0xFFFF));
    }

    @Override
    protected int getModelId() {
        return MODEL_ID;
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
