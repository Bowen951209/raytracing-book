package net.bowen.draw.textures;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class SolidTexture extends Texture {
    private static final List<Color> COLORS = new ArrayList<>();
    private static SolidTexture texture;
    private static int textureIndex;

    private SolidTexture() {
        super(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE);
    }

    @Override
    protected int getTextureTypeId() {
        return SOLID;
    }

    public static void init() {
        texture = new SolidTexture();
        texturesInComputeAdd(texture);

        textureIndex = TEXTURES_IN_COMPUTE.indexOf(texture);
    }

    public static int registerColor(float r, float g, float b) {
        return registerColor(new Color(r, g, b));
    }

    public static int registerColor(net.bowen.draw.Color color) {
        return registerColor(color.getAWTColor());
    }

    public static int registerColor(Color color) {
        COLORS.add(color);
        int detail = COLORS.size() - 1;
        return getValue(SOLID, textureIndex, detail);
    }

    public static void putDataToTexture() {
        if (texture == null) throw new IllegalStateException("SolidTexture not initialized.");

        ByteBuffer buffer = MemoryUtil.memAlloc(COLORS.size() * 3);
        for (Color color : COLORS) {
            buffer.put((byte) color.getRed());
            buffer.put((byte) color.getGreen());
            buffer.put((byte) color.getBlue());
        }

        buffer.flip();
        texture.putData(COLORS.size(), 1, buffer);
        MemoryUtil.memFree(buffer);
    }
}
