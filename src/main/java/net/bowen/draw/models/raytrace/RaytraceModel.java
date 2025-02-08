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
    public static int CONSTANT_MEDIUM_ID = 3;
    public static int BOX_ID = 4;

    public static final List<BVHNode> BVH_NODES = new ArrayList<>();

    private static final List<RaytraceModel> ALL_MODELS = new ArrayList<>();
    private static final List<Sphere> SPHERES = new ArrayList<>();
    private static final List<Quad> QUADS = new ArrayList<>();
    private static final List<ConstantMedium> CONSTANT_MEDIUMS = new ArrayList<>();
    private static final List<Box> BOXES = new ArrayList<>();
    private static final List<RaytraceModel> LIGHTS = new ArrayList<>();

    private static BufferObject sphereSSBO, quadSSBO, boxesSSBO, constantMediumSSBO, bvhSSBO, lightsSSBO;

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

    public static void addLight(RaytraceModel light) {
        LIGHTS.add(light);
    }

    public static void addModel(RaytraceModel model) {
        if (model instanceof Sphere sphere) {
            SPHERES.add(sphere);
            model.indexInList = SPHERES.size() - 1;
        } else if (model instanceof Quad quad) {
            QUADS.add(quad);
            model.indexInList = QUADS.size() - 1;
        } else if (model instanceof Box box) {
            BOXES.add(box);
            model.indexInList = BOXES.size() - 1;
        } else if (model instanceof ConstantMedium constantMedium) {
            CONSTANT_MEDIUMS.add(constantMedium);
            model.indexInList = CONSTANT_MEDIUMS.size() - 1;
            // Remove constant medium's boundary from ALL_MODELS
            ALL_MODELS.remove(constantMedium.getBoundary());
        } else if (model == null) {
            throw new RuntimeException("Model cannot be null.");
        } else {
            throw new RuntimeException("Unknown model type.");
        }

        ALL_MODELS.add(model);
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

        // Quads:
        quadSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, quadSSBO.getId());

        // Boxes:
        boxesSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, boxesSSBO.getId());

        // Constant Mediums:
        constantMediumSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, constantMediumSSBO.getId());

        // Lights:
        lightsSSBO = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, lightsSSBO.getId());
    }

    public static void putModelsToProgram() {
        sphereSSBO.bind();
        putSpheresToProgram();

        quadSSBO.bind();
        putQuadsToProgram();

        boxesSSBO.bind();
        putBoxesToProgram();

        constantMediumSSBO.bind();
        putConstantMediumsToProgram();

        lightsSSBO.bind();
        putLightsToProgram();

        // Recursively create BVH nodes for models. Each node will put itself to the BVH_NODES list.
        new BVHNode(ALL_MODELS, 0, ALL_MODELS.size());

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

    private static void putBoxesToProgram() {
        // A box is composed if 6 quads, and quad structure is describe in #putQuadsToProgram.
        ByteBuffer buffer = MemoryUtil.memAlloc(BOXES.size() * 120 * Float.BYTES);
        for (Box box : BOXES)
            box.putToBuffer(buffer);
        buffer.flip();

        if (boxesSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        boxesSSBO.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }

    private static void putConstantMediumsToProgram() {
        // - 1 int for boundary model index in its SSBO.
        // - 1 int for boundary model type.
        // - 1 float for negative inverse density.
        // - 1 int for material packed value.
        // - 1 int for texture id.

        ByteBuffer buffer = MemoryUtil.memAlloc(CONSTANT_MEDIUMS.size() * 5 * Float.BYTES);
        for (ConstantMedium constantMedium : CONSTANT_MEDIUMS)
            constantMedium.putToBuffer(buffer);
        buffer.flip();

        if (constantMediumSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        constantMediumSSBO.uploadData(buffer, GL_STATIC_DRAW);
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

    private static void putLightsToProgram() {
        ByteBuffer buffer = MemoryUtil.memAlloc((1 + LIGHTS.size()) * Integer.BYTES);
        buffer.putInt(LIGHTS.size());
        for (RaytraceModel light : LIGHTS) {
            // pack model type and model index in a single int.
            buffer.putInt(light.getModelId() << 16 | light.indexInList);
        }
        buffer.flip();

        if (lightsSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        lightsSSBO.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }
}
