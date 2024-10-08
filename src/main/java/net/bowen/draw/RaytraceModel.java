package net.bowen.draw;

import net.bowen.draw.material.Material;
import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.opengl.GL43.*;

public abstract class RaytraceModel {
    private static final Set<RaytraceModel> MODELS = new HashSet<>();

    private static BufferObject ssbo;

    protected float[] data;

    private final Material material;

    protected RaytraceModel(Material material) {
        this.material = material;
    }

    public static void addModel(RaytraceModel model) {
        MODELS.add(model);
    }

    public static void initSSBO() {
        // Init the ssbo we want to pass data to program through.
        ssbo = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        ssbo.bind();
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssbo.getId());
    }

    public static void putModelsToProgram() {
        // TODO: Things are only for spheres here. Adapt to other models in the future.
        // - 3 floats for center (vec3)
        // - 3 floats for center vector (vec3)
        // - 1 float for radius
        // - 3 floats for albedo (vec3)
        // - 1 float for material
        FloatBuffer buffer = MemoryUtil.memAllocFloat(MODELS.size() * 12 + 4);
        buffer.put(MODELS.size()); // the length of the models
        buffer.put(0f).put(0f).put(0f); // paddings
        for (RaytraceModel model : MODELS) {
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

        if (ssbo == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        ssbo.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }
}
