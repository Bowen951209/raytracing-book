package net.bowen.draw.materials;

import net.bowen.draw.textures.Texture;

public class Lambertian extends Material {
    public Lambertian(Texture texture) {
        super(LAMBERTIAN, texture);
    }

    public Lambertian(int texturePackedValue) {
        super(LAMBERTIAN, texturePackedValue);
    }
}
