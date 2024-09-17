package net.bowen.draw;

import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public abstract class RaytraceModel {
    private static final List<RaytraceModel> MODELS = new ArrayList<>();

    private static BufferObject ssbo;

    protected float[] data;

    public static void addModel(RaytraceModel model) {
        MODELS.add(model);
    }

    public static void initSSBO() {
        // Init the ssbo we want to pass data to program through.
        ssbo = new BufferObject();
        ssbo.bind(GL_SHADER_STORAGE_BUFFER);
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssbo.getId());
    }

    public static void putModelsToProgram() {
        // TODO: Things are only for spheres here. Adapt to other models in the future.
        // - 3 floats for center (vec3)
        // - 1 float for radius
        // - 3 floats for albedo (vec3)
        // - 1 float for material
        FloatBuffer buffer = MemoryUtil.memAllocFloat(MODELS.size() * 8);
        for (RaytraceModel model : MODELS) {
            // Center position (vec3)
            buffer.put(model.data[0]).put(model.data[1]).put(model.data[2]); // x, y, z

            // Radius (float)
            buffer.put(model.data[3]);

            // Albedo (vec3)
            buffer.put(model.data[5]).put(model.data[6]).put(model.data[7]); // r, g, b

            // Material (float)
            buffer.put(model.data[4]);
        }
        buffer.flip();

        ssbo.uploadData(GL_SHADER_STORAGE_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }
}
