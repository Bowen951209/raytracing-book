package net.bowen.draw.material;

public class Metal extends Material{

    private final float fuzz;

    /**
     * @param fuzz The fuzz value of tha material. Need to be in range [0, 1).
     */
    public Metal(float albedoR, float albedoG, float albedoB, float fuzz) {
        super(METAL, albedoR, albedoG, albedoB);
        if (fuzz < 0 || fuzz >= 1)
            throw new IllegalArgumentException("Fuzz value should always in range [0, 1).");
        this.fuzz = fuzz;
    }

    @Override
    public float getValue() {
        return materialId + fuzz;
    }
}
