package net.bowen.draw.material;

public abstract class Material {
    public static final int LAMBERTIAN = 0;
    public static final int METAL = 1;

    protected final int materialId;
    protected final float[] albedo;

    public Material(int materialId, float albedoR, float albedoG, float albedoB) {
        this.materialId = materialId;
        albedo = new float[] {albedoR, albedoG, albedoB};
    }

    public float getValue() {
        return materialId;
    }

    public float[] getAlbedo() {
        return albedo;
    }
}
