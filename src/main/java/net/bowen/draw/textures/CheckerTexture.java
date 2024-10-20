package net.bowen.draw.textures;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL43.*;

public class CheckerTexture extends Texture {
    private final float scale;

    private CheckerTexture(ByteBuffer buffer, float scale) {
        super(2, 1, GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, buffer);
        this.scale = scale;
    }

    @Override
    public int getValue() {
        // Quantize the scale value (assuming scale is in the range [0, 1])
        int scaleQuantized = (int) (scale * 65535.0f); // Quantize float to 16 bits (range 0-65535)

        // Pack the texture ID (16 bits) and scaleQuantized (16 bits) into a single 32-bit integer
        return super.getValue() | (scaleQuantized & 0xFFFF);
    }

    public static CheckerTexture create(float r1, float g1, float b1, float r2, float g2, float b2, float scale) {
        return create(new Color(r1, g1, b1), new Color(r2, g2, b2), scale);
    }

    public static CheckerTexture create(Color color1, Color color2, float scale) {
        ByteBuffer buffer = MemoryUtil.memAlloc(6); // width(2) * height(1) * rgb(3) = 6
        buffer.put((byte) color1.getRed()).put((byte) color1.getGreen()).put((byte) color1.getBlue());
        buffer.put((byte) color2.getRed()).put((byte) color2.getGreen()).put((byte) color2.getBlue());
        buffer.flip();

        CheckerTexture instance = new CheckerTexture(buffer, scale);
        MemoryUtil.memFree(buffer);

        texturesInComputeAdd(instance);
        return instance;
    }
}
