package net.bowen.draw.materials;

import net.bowen.draw.Color;

public class DiffuseLight extends Material {
    private final Color color;


    public DiffuseLight(Color color) {
        super(DIFFUSE_LIGHT, 0);
        this.color = color;
    }

    @Override
    public Color emitted() {
        return color;
    }
}
