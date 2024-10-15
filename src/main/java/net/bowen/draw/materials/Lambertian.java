package net.bowen.draw.materials;

import net.bowen.draw.Color;
import net.bowen.draw.textures.Texture;

public class Lambertian extends Material{
    public Lambertian(float albedoR, float albedoG, float albedoB) {
        super(LAMBERTIAN, albedoR, albedoG, albedoB);
    }

    public Lambertian(Color color) {
        super(LAMBERTIAN, color);
    }

    // TODO: Make the constructors reusable.
    public Lambertian(Texture texture) {
        super(LAMBERTIAN, texture);
    }
}
