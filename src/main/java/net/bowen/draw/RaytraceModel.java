package net.bowen.draw;

import net.bowen.draw.material.Material;
import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.opengl.GL43.*;

public abstract class RaytraceModel {
    private static final Set<Sphere> SPHERES = new HashSet<>();

    private static BufferObject sphereSSBO, bvhSSBO;

    protected float[] data;

    private final Material material;

    protected RaytraceModel(Material material) {
        this.material = material;
    }

    public static void addModel(RaytraceModel model) {
        // assume it always be sphere
        SPHERES.add((Sphere) model);
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

        bvhSSBO.bind();
        putBVHNodesToProgram();
    }

    private static void putSpheresToProgram() {
        // - 3 floats for center (vec3)
        // - 3 floats for center vector (vec3)
        // - 1 float for radius
        // - 3 floats for albedo (vec3)
        // - 1 float for material
        FloatBuffer buffer = MemoryUtil.memAllocFloat(SPHERES.size() * 12 + 4);
        buffer.put(SPHERES.size()); // the length of the models
        buffer.put(0f).put(0f).put(0f); // paddings
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
        FloatBuffer buffer = MemoryUtil.memAllocFloat(SPHERES.size());
        for (int i = 0; i < SPHERES.size(); i++) {
            buffer.put(i);
        }
        buffer.flip();

        if (bvhSSBO == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        bvhSSBO.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }
}
