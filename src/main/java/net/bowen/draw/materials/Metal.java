package net.bowen.draw.materials;

import net.bowen.draw.textures.Texture;

public class Metal extends Material {
    private final float fuzz;

    /**
     * @param fuzz The fuzz value of tha material. Need to be in range [0, 1).
     */
    public Metal(Texture texture, float fuzz) {
        super(METAL, texture);
        if (fuzz < 0 || fuzz >= 1)
            throw new IllegalArgumentException("Fuzz value should always in range [0, 1).");
        this.fuzz = fuzz;
    }

    /**
     * @param fuzz The fuzz value of tha material. Need to be in range [0, 1).
     */
    public Metal(int texturePackedValue, float fuzz) {
        super(METAL, texturePackedValue);
                if (fuzz < 0 || fuzz >= 1)
            throw new IllegalArgumentException("Fuzz value should always in range [0, 1).");
        this.fuzz = fuzz;
    }

    @Override
    public int getMaterialPackedValue() {
        // Quantize the fuzz value (assuming fuzz is in the range [0, 1])
        int fuzzQuantized = (int) (fuzz * 65535.0f); // Quantize float to 16 bits (range 0-65535)

        // Pack the material ID (16 bits) and fuzzQuantized (16 bits) into a single 32-bit integer
        return super.getMaterialPackedValue() | (fuzzQuantized & 0xFFFF);
    }
}
