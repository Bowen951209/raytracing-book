package net.bowen.draw;

import net.bowen.draw.material.Material;
import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public abstract class RaytraceModel {
    public static final List<Sphere> SPHERES = new ArrayList<>();
    public static final List<BVHNode> BVH_NODES = new ArrayList<>();

    private static BufferObject sphereSSBO, bvhSSBO;

    private final Material material;

    /**
     * The id of this model. Number in the integer digit is its index in the list; number in the floating digit is the
     * model id. For example, id 5.1 stands for sphere at index 5, and id 13.0 stands for BVH node at index 13, in their
     * model lists respectively.
     */
    public float id;

    protected float[] data;
    protected AABB bbox;

    protected RaytraceModel(Material material) {
        this.material = material;
    }

    protected RaytraceModel() {
        material = null;
    }

    public AABB boundingBox() {
        return bbox;
    }

    public static void addModel(RaytraceModel model) {
        // Since we only have sphere models now, we assume it is always sphere.
        SPHERES.add((Sphere) model);

        // The id can be calculated by the size of the list. And remember we add the model id to the floating point.
        model.id = SPHERES.size() - 1 + Sphere.MODEL_ID;
    }

    public static void initSSBO() {
        // Init the ssbo we want to pass data to program through.

        // Spheres:
        sphereSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, sphereSSBO.getId());

        // BVH nodes:
        bvhSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, bvhSSBO.getId());
    }

    public static void putModelsToProgram() {
        sphereSSBO.bind();
        putSpheresToProgram();

        // Recursively create BVH nodes. Each node will put itself to the BVH_NODES list.
        new BVHNode(SPHERES, 0, SPHERES.size());

        bvhSSBO.bind();
        putBVHNodesToProgram();
    }

    private static void putSpheresToProgram() {
        // - 3 floats for center (vec3)
        // - 3 floats for center vector (vec3)
        // - 1 float for radius
        // - 3 floats for albedo (vec3)
        // - 1 float for material
        FloatBuffer buffer = MemoryUtil.memAllocFloat(SPHERES.size() * 12);
        for (RaytraceModel model : SPHERES) {
            // Center position (vec3)
            buffer.put(model.data[0]).put(model.data[1]).put(model.data[2]); // x, y, z
            buffer.put(0); // padding

            // Center vector (vec3)
            buffer.put(model.data[3]).put(model.data[4]).put(model.data[5]);

            // Radius (float)
            buffer.put(model.data[6]);

            // Albedo (vec3)
            buffer.put(model.material.getAlbedo()); // r, g, b

            // Material (float)
            buffer.put(model.material.getValue());
        }
        buffer.flip();

        if (sphereSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        sphereSSBO.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }

    private static void putBVHNodesToProgram() {
        // - 2 floats for x interval.
        // - 2 floats for y interval.
        // - 2 floats for z interval.
        // - 1 float for left id.
        // - 1 float for right id.
        FloatBuffer buffer = MemoryUtil.memAllocFloat(BVH_NODES.size() * 8);
        for (BVHNode bvhNode : BVH_NODES) {
            bvhNode.putToBuffer(buffer);
        }
        buffer.flip();

        if (bvhSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        bvhSSBO.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }
}
