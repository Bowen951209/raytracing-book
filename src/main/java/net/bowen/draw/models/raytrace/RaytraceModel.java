package net.bowen.draw.models.raytrace;

import net.bowen.draw.materials.Material;
import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public abstract class RaytraceModel {
    public static int BVH_NODE_ID = 0;
    public static int SPHERE_ID = 1;
    public static int QUAD_ID = 2;

    public static final List<Sphere> SPHERES = new ArrayList<>();
    public static final List<Quad> QUADS = new ArrayList<>();
    public static final List<BVHNode> BVH_NODES = new ArrayList<>();
    private static BufferObject sphereSSBO, quadSSBO, bvhSSBO;

    protected final Material material;

    public int indexInList;
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

    protected int getModelId() {
        //  If this method is not overridden, the model should not have a valid model id.
        return -1;
    }

    public static void addModel(RaytraceModel model) {
        if (model instanceof Sphere) {
            SPHERES.add((Sphere) model);
            // The id can be calculated by the size of the list. And remember we add the model id to the floating point.
            model.indexInList = SPHERES.size() - 1;
        } else if (model instanceof Quad) {
            QUADS.add((Quad) model);
            // The id can be calculated by the size of the list. And remember we add the model id to the floating point.
            model.indexInList = QUADS.size() - 1;
        }
    }

    public static void addModel(List<? extends RaytraceModel> model) {
        model.forEach(RaytraceModel::addModel);
    }

    public static void initSSBOs() {
        // Init the ssbo we want to pass data to program through.

        // Spheres:
        sphereSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, sphereSSBO.getId());

        // Quads:
        quadSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, quadSSBO.getId());

        // BVH nodes:
        bvhSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, bvhSSBO.getId());
    }

    public static void putModelsToProgram() {
        sphereSSBO.bind();
        putSpheresToProgram();

        quadSSBO.bind();
        putQuadsToProgram();

        // Create a list of all models.
        List<RaytraceModel> allModels = new ArrayList<>();
        allModels.addAll(SPHERES);
        allModels.addAll(QUADS);

        // Recursively create BVH nodes for models. Each node will put itself to the BVH_NODES list.
        new BVHNode(allModels, 0, allModels.size());

        bvhSSBO.bind();
        putBVHNodesToProgram();
    }

    private static void putSpheresToProgram() {
        // - 3 floats for center (vec3)
        // - 1 int for material id.
        // - 3 floats for center vector (vec3)
        // - 1 float for radius
        // - 1 int for material
        // - 4 int paddings
        ByteBuffer buffer = MemoryUtil.memAlloc(SPHERES.size() * 12 * Float.BYTES);
        for (Sphere sphere : SPHERES)
            sphere.putToBuffer(buffer);
        buffer.flip();

        if (sphereSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        sphereSSBO.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }

    private static void putQuadsToProgram() {
        // - 3 floats for normal (vec3)
        // - 1 float for d
        // - 3 floats for q (vec3)
        // - 1 int for material id
        // - 3 floats for u (vec3)
        // - 1 int for texture id
        // - 3 floats for v (vec3)
        // - 1 int padding
        // - 3 floats for emission (vec3)
        // - 1 int padding
        ByteBuffer buffer = MemoryUtil.memAlloc(QUADS.size() * 20 * Float.BYTES);
        for (Quad quad : QUADS)
            quad.putToBuffer(buffer);
        buffer.flip();

        if (quadSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        quadSSBO.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }

    private static void putBVHNodesToProgram() {
        // - 2 floats for x interval.
        // - 2 floats for y interval.
        // - 2 floats for z interval.
        // - 1 float for left id.
        // - 1 float for right id.
        ByteBuffer buffer = MemoryUtil.memAlloc(BVH_NODES.size() * 8 * Float.BYTES);
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
