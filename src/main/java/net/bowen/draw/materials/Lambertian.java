package net.bowen.draw.materials;

import net.bowen.draw.Color;

public class Lambertian extends Material{
    public Lambertian(float albedoR, float albedoG, float albedoB) {
        super(LAMBERTIAN, albedoR, albedoG, albedoB);
    }

    public Lambertian(Color color) {
        super(LAMBERTIAN, color);
    }
}
