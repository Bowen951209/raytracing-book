package net.bowen.draw.textures;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL43.*;

/**
 * <p>
 * A Perlin noise consists of 4 arrays: 1 float array of random values, 3 int arrays of permutation indices. Each
 * array length is 256.
 * </p>
 *
 * <p>
 *  The arrays are stored in a 4 x 256-sized texture unit. The arrangement is as follows:
 * </p>
 *
 * <pre>
 *   - Random values are in row 0.
 *   - Permutation x are in row 1.
 *   - Permutation y are in row 2.
 *   - Permutation z are in row 3.
 * </pre>
 *
 * Because each pixel in a texture should be in the same data type, the int arrays are stored as floats with
 * fractional digits of 0, in order to be aligned with the random float array.
 */
public class PerlinNoiseTexture extends Texture {
    private static final int TEXTURE_TYPE_ID = 3;
    private static final int POINT_COUNT = 256;

    private PerlinNoiseTexture(ByteBuffer data) {
        super(4, POINT_COUNT, GL_R32F, GL_RED, GL_FLOAT, data);
    }

    @Override
    protected int getTextureTypeId() {
        return TEXTURE_TYPE_ID;
    }

    public static PerlinNoiseTexture create() {
        ByteBuffer data = MemoryUtil.memAlloc(4 * POINT_COUNT * Float.BYTES);

        Noise noise = new Noise();
        for (int i = 0; i < POINT_COUNT; i++) {
            data.putFloat(noise.randomFloats.get(i));
            data.putFloat(noise.permX.get(i));
            data.putFloat(noise.permY.get(i));
            data.putFloat(noise.permZ.get(i));
        }
        data.flip();

        PerlinNoiseTexture instance = new PerlinNoiseTexture(data);
        MemoryUtil.memFree(data);
        texturesInComputeAdd(instance);
        return instance;
    }

    private static class Noise {
        private final Random random = new Random();
        private final List<Float> randomFloats = new ArrayList<>(POINT_COUNT);
        private final List<Integer> permX = new ArrayList<>(POINT_COUNT);
        private final List<Integer> permY = new ArrayList<>(POINT_COUNT);
        private final List<Integer> permZ = new ArrayList<>(POINT_COUNT);

        public Noise() {
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
    }
}
