package net.bowen.draw.materials;

import net.bowen.draw.Color;
import net.bowen.draw.textures.Texture;

public abstract class Material {
    public static final int LAMBERTIAN = 0;
    public static final int METAL = 1;
    public static final int DIELECTRIC = 2;
    public static final int DIFFUSE_LIGHT = 3;
    public static final int ISOTROPIC = 4;

    protected final int materialId;
    private final int texturePackedValue;

    public Material(int materialId, Texture texture) {
        this.materialId = materialId;
        this.texturePackedValue = texture.getValue();
    }

    public Material(int materialId, int texturePackedValue) {
        this.materialId = materialId;
        this.texturePackedValue = texturePackedValue;
    }

    public Color emitted()  {
        return new Color(0, 0, 0);
    }

    /**
     * @return A packed value where upper 16 bits is the material id, and the lower 16 bits is whatever data a type
     * of texture want to store. For example, metal stores its fuzz value in the lower 16 bits.
     */
    public int getMaterialPackedValue() {
        return materialId << 16;
    }

    /**
     * @return The packed int of texture.
     * @see Texture#getValue()
     */
    public int getTexturePackedValue() {
        return texturePackedValue;
    }
}
