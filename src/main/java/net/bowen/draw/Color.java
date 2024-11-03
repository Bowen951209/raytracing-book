package net.bowen.draw;

import java.util.Random;

public class Color {
    private static final Random RANDOM = new Random();
    public float r, g, b;

    public Color(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static Color randomColor() {
        return new Color(RANDOM.nextFloat(), RANDOM.nextFloat(), RANDOM.nextFloat());
    }

    public static Color randomColor(float min, float max) {
        return new Color(RANDOM.nextFloat(min, max), RANDOM.nextFloat(min, max), RANDOM.nextFloat(min, max));
    }

    public java.awt.Color getAWTColor() {
        return new java.awt.Color(r, g, b);
    }

    public Color mul(Color c){
        r *= c.r;
        g *= c.g;
        b *= c.b;
        return this;
    }
}
