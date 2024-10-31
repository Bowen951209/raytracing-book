package net.bowen.draw.textures;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL43.*;

/**
 * <p>
 * A Perlin noise consists of 6 arrays: 3 float arrays of random vectors' xyz components, 3 int arrays of permutation
 * xyz indices. Each array length is 256.
 * </p>
 *
 * <p>
 * The arrays are stored in a 6 x 256-sized texture unit. The arrangement is as follows:
 * </p>
 *
 * <pre>
 *   - Random vector x are in row 0.
 *   - Random vector y are in row 1.
 *   - Random vector z are in row 2.
 *   - Permutation x are in row 3.
 *   - Permutation y are in row 4.
 *   - Permutation z are in row 5.
 * </pre>
 * <p>
 * Because each pixel in a texture should be in the same data type, the permutation int arrays are stored as floats with
 * fractional digits of 0, in order to be aligned with the random vector arrays.
 */
public class PerlinNoiseTexture extends Texture {
    private static final int TEXTURE_TYPE_ID = 3;
    private static final int POINT_COUNT = 256;

    private final float depth;

    private PerlinNoiseTexture(ByteBuffer data, float depth) {
        super(6, POINT_COUNT, GL_R32F, GL_RED, GL_FLOAT, data);
        this.depth = depth;
    }

    @Override
    protected int getTextureTypeId() {
        return TEXTURE_TYPE_ID;
    }

    @Override
    protected float getDetail() {
        // The shader can only interpret detail values in range [0, 1]
        // , so we downscale it, and then upscale back in the shader.
        return depth / 100f;
    }

    public static PerlinNoiseTexture create(float depth) {
        ByteBuffer data = MemoryUtil.memAlloc(6 * POINT_COUNT * Float.BYTES);

        Noise noise = new Noise();
        for (int i = 0; i < POINT_COUNT; i++) {
            data.putFloat(noise.randomVectors[i].x);
            data.putFloat(noise.randomVectors[i].y);
            data.putFloat(noise.randomVectors[i].z);

            data.putFloat(noise.permX.get(i));
            data.putFloat(noise.permY.get(i));
            data.putFloat(noise.permZ.get(i));
        }
        data.flip();

        PerlinNoiseTexture instance = new PerlinNoiseTexture(data, depth);
        MemoryUtil.memFree(data);
        texturesInComputeAdd(instance);
        return instance;
    }

    private static class Noise {
        private final Random random = new Random();
        private final Vector3f[] randomVectors = new Vector3f[POINT_COUNT];
        private final List<Integer> permX = new ArrayList<>(POINT_COUNT);
        private final List<Integer> permY = new ArrayList<>(POINT_COUNT);
        private final List<Integer> permZ = new ArrayList<>(POINT_COUNT);

        public Noise() {
            for (int i = 0; i < POINT_COUNT; i++) {
                randomVectors[i] = new Vector3f(
                        random.nextFloat(-1, 1),
                        random.nextFloat(-1, 1),
                        random.nextFloat(-1, 1)
                );
            }

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
