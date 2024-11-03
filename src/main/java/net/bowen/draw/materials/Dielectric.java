package net.bowen.draw.materials;

import net.bowen.draw.textures.SolidTexture;

import java.awt.*;

public class Dielectric extends Material {
    private static final float MIN_IOR = 1.0f;
    private static final float MAX_IOR = 2.5f;


    private final float indexOfRefraction;

    public Dielectric(float indexOfRefraction) {
        super(DIELECTRIC, SolidTexture.registerColor(Color.WHITE));
        this.indexOfRefraction = indexOfRefraction;
    }

    @Override
    public int getMaterialPackedValue() {
        // Normalize the IOR to [0, 1]
        float normalizedIOR = (indexOfRefraction - MIN_IOR) / (MAX_IOR - MIN_IOR);

        // Quantize the IOR to fit in 16 bits (range 0-65535)
        int iorQuantized = (int) (normalizedIOR * 65535.0f);

        // Pack the materialID (upper 16 bits) and iorQuantized (lower 16 bits) into a 32-bit integer
        return super.getMaterialPackedValue() | (iorQuantized & 0xFFFF);
    }
}
