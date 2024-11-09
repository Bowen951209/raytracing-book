package net.bowen.draw.textures;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public class CheckerTexture extends Texture {
    private static final List<Color> COLOR1 = new ArrayList<>();
    private static final List<Color> COLOR2 = new ArrayList<>();
    private static final List<Float> SCALES = new ArrayList<>();
    private static CheckerTexture texture;
    private static int textureIndex;

    private CheckerTexture() {
        super(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE);
    }

    public static void init() {
        texture = new CheckerTexture();
        texturesInComputeAdd(texture);
        textureIndex = TEXTURES_IN_COMPUTE.indexOf(texture);
    }

    /**
     * Register a checkerboard data set into the checkerboards' public texture. Note that the scale value will only be
     * 1 byte in precision.
     */
    public static int registerColor(float r1, float g1, float b1, float r2, float g2, float b2, float scale) {
        return registerColor(new Color(r1, g1, b1), new Color(r2, g2, b2), scale);
    }

    public static int registerColor(Color color1, Color color2, float scale) {
        COLOR1.add(color1);
        COLOR2.add(color2);
        SCALES.add(scale);

        int detail = COLOR1.size() - 1; // detail is the index in the texture
        return getValue(CHECKER, textureIndex, detail);
    }

    public static void putDataToTexture() {
        if (texture == null) throw new IllegalStateException("CheckerTexture not initialized.");

        ByteBuffer buffer = MemoryUtil.memAlloc(COLOR1.size() * 12);
        for (int i = 0; i < COLOR1.size(); i++) {
            Color color1 = COLOR1.get(i);
            Color color2 = COLOR2.get(i);
            float scale = SCALES.get(i);

            buffer.put((byte) (color1.getRed()));
            buffer.put((byte) (color1.getGreen()));
            buffer.put((byte) (color1.getBlue()));

            buffer.put((byte) (color2.getRed()));
            buffer.put((byte) (color2.getGreen()));
            buffer.put((byte) (color2.getBlue()));

            // The scale value is only 1 byte in precision.
            buffer.put((byte) (scale * 255));
            buffer.put((byte) 0).put((byte) 0);
        }

        buffer.flip();
        texture.putData(COLOR1.size() * 3, 1, buffer);
        MemoryUtil.memFree(buffer);
    }

    @Override
    protected int getTextureTypeId() {
        return CHECKER;
    }
}
