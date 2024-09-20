package net.bowen.draw.material;

public class Lambertian extends Material{
    public Lambertian(float albedoR, float albedoG, float albedoB) {
        super(LAMBERTIAN, albedoR, albedoG, albedoB);
    }
}
