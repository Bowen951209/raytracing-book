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
        // The fuzz value is stored in a strange form because I want to keep the memory arrangement in openGL simple.
        // Every material is stored as a float. The integer digit is the id of the material, and for this metal material
        // case, the floating point digits are the fuzz value. For example, metal material with fuzz value of 0.3 will
        // be represented as 1.3, where 1 is the id of the metal material.
        return materialId + fuzz;
    }
}
