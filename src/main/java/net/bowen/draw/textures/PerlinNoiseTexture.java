package net.bowen.draw.textures;

import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL43.*;


/**
 * The {@link PerlinNoiseTexture} class inherits from {@link Texture} but its data is stored in a SSBO rather than an
 * OpenGL texture unit. This is because the data structure is very different from a 2D texture. We consider Perlin
 * noises as textures in the compute shader because it is a part of the texture mapping.
 */
public class PerlinNoiseTexture extends Texture {
    private static final int TEXTURE_TYPE_ID = 3;
    private static final int POINT_COUNT = 256;
    private static final List<PerlinNoiseTexture> INSTANCES = new ArrayList<>();

    private static BufferObject ssbo;

    public int index;

    private final Random random = new Random();
    private final List<Float> randomFloats = new ArrayList<>(POINT_COUNT);
    private final List<Integer> permX = new ArrayList<>(POINT_COUNT);
    private final List<Integer> permY = new ArrayList<>(POINT_COUNT);
    private final List<Integer> permZ = new ArrayList<>(POINT_COUNT);

    public PerlinNoiseTexture() {
        for (int i = 0; i < POINT_COUNT; i++)
            randomFloats.add(random.nextFloat());

        perlinGeneratePerm(permX);
        perlinGeneratePerm(permY);
        perlinGeneratePerm(permZ);
    }

    private void perlinGeneratePerm(List<Integer> p) {
        for (int i = 0; i < POINT_COUNT; i++)
            p.add(i);

        Collections.shuffle(p, random);
    }

    @Override
    protected int getTextureTypeId() {
        return TEXTURE_TYPE_ID;
    }

    public static void addInstance(PerlinNoiseTexture instance) {
        INSTANCES.add(instance);
        instance.index = INSTANCES.size() - 1;
    }

    public static void initSSBO() {
        ssbo = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        // Bind the UBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, ssbo.getId());
    }

    public static void putAllToProgram() {
        ByteBuffer buffer = MemoryUtil.memAlloc(4 * POINT_COUNT * INSTANCES.size() * Float.BYTES);

        for (PerlinNoiseTexture instance : INSTANCES) {
            instance.randomFloats.forEach(buffer::putFloat);
            instance.permX.forEach(buffer::putInt);
            instance.permY.forEach(buffer::putInt);
            instance.permZ.forEach(buffer::putInt);
        }
        buffer.flip();

        if (ssbo == null)
            throw new NullPointerException("ssbo is null. Has it been initialized?");

        ssbo.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }
}
