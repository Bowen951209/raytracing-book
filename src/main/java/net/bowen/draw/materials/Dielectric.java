package net.bowen.draw.materials;

public class Dielectric extends Material {
    private final float indexOfRefraction;
    public Dielectric(float indexOfRefraction) {
        super(DIELECTRIC, 1.0f, 1.0f, 1.0f);
        this.indexOfRefraction = indexOfRefraction;
    }

    @Override
    public float getValue() {
        // The floating point digits are the indexOfRefraction value. And because the value may be bigger than 1, so we
        // move it to the next digit to prevent the carry. For example, indexOfRefraction value of 1.5 will be
        // represented as 2.15, where 2 is the id of the dielectric material.
        return DIELECTRIC + indexOfRefraction * 0.1f;
    }
}
