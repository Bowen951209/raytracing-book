package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;
import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public abstract class RaytraceModel {
    public static final List<Sphere> SPHERES = new ArrayList<>();
    public static final List<BVHNode> BVH_NODES = new ArrayList<>();

    private static BufferObject sphereSSBO, bvhSSBO;

    private final Material material;

    public int indexInList;

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
        model.indexInList = SPHERES.size() - 1;
    }

    public static void initSSBOs() {
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
        // - 1 int for material id.
        // - 3 floats for center vector (vec3)
        // - 1 float for radius
        // - 3 floats for albedo (vec3)
        // - 1 float for material
        ByteBuffer buffer = MemoryUtil.memAlloc(SPHERES.size() * 12 * Byte.SIZE);
        for (RaytraceModel model : SPHERES) {
            // Center position (vec3)
            buffer.putFloat(model.data[0]).putFloat(model.data[1]).putFloat(model.data[2]); // x, y, z

            // Material id.
            buffer.putInt(model.material.getTextureValue());

            // Center vector (vec3)
            buffer.putFloat(model.data[3]).putFloat(model.data[4]).putFloat(model.data[5]);

            // Radius (float)
            buffer.putFloat(model.data[6]);

            // Albedo (vec3)
            float[] albedo = model.material.getAlbedo();
            buffer.putFloat(albedo[0]); // r
            buffer.putFloat(albedo[1]); // g
            buffer.putFloat(albedo[2]); // b

            // Material (int)
            buffer.putInt(model.material.getValue());
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
        ByteBuffer buffer = MemoryUtil.memAlloc(BVH_NODES.size() * 8 * Byte.SIZE);
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
