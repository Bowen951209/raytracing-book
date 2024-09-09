package net.bowen.draw;

import static org.lwjgl.opengl.GL15.*;

public class Quad extends Drawable {
    private static final int[] INDICES = {
            0, 1, 2,  // First triangle
            2, 3, 0   // Second triangle
    };

    public Quad(float x, float y, float w, float h) {
        super(new float[]{
                // Positions   // Tex coords
                x, y - h, 0.0f, -1.0f, -1.0f,    // Bottom-left
                x + w, y - h, 0.0f, 1.0f, -1.0f, // Bottom-right
                x + w, y, 0.0f, 1.0f, 1.0f,      // Top-right
                x, y, 0.0f, 0.0f, 1.0f           // Top-left
        }, INDICES);
    }

    @Override
    public void draw() {
        vao.bind();
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        vao.unbind();
    }

    @Override
    protected void setVertexAttributePointer() {
        // Set up vertex attribute pointers

        // Position attribute
        vbo.setVertexAttributePointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);

        // Texture coordinates attribute
        vbo.setVertexAttributePointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
    }
}
