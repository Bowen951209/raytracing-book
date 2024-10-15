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

    public float getValue() {
        // The material value is stored in a strange form of float because I want to keep the memory arrangement in
        // openGL simple. Each material has the integer digit for its material id, and the floating points for its
        // special properties. For example, metal material has its floating point as the fuzz value.
        return materialId;
    }

    public float[] getAlbedo() {
        return albedo != null ? albedo : new float[3];
    }

    /**
     * @return The texture index in integer digits, and the detail information in float digits.
     */
    public float getTextureId() {
        return texture != null ? texture.getId() + 1 : -1;
    }
}
