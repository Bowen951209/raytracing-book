package net.bowen.draw.materials;

import net.bowen.draw.Color;
import net.bowen.draw.textures.Texture;

public abstract class Material {
    public static final int LAMBERTIAN = 0;
    public static final int METAL = 1;
    public static final int DIELECTRIC = 2;

    protected final int materialId;

    protected float[] albedo;
    protected Texture texture;

    public Material(int materialId, Color color) {
        this(materialId, color.r, color.g, color.b);
    }

    public Material(int materialId, float albedoR, float albedoG, float albedoB) {
        this.materialId = materialId;
        albedo = new float[] {albedoR, albedoG, albedoB};
    }

    public Material(int materialId, Texture texture) {
        this.materialId = materialId;
        this.texture = texture;
    }

    /**
     * @return A packed value where upper 16 bits is the material id, and the lower 16 bits is whatever data a type
     * of texture want to store. For example, metal stores its fuzz value in the lower 16 bits.
     */
    public int getValue() {
        return materialId << 16;
    }

    public float[] getAlbedo() {
        return albedo != null ? albedo : new float[3];
    }

    /**
     * @return The packed int of texture.
     * @see Texture#getValue()
     */
    public int getTextureValue() {
        return texture != null ? texture.getValue() + 1 : -1;
    }
}
