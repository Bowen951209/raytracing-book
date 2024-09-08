package net.bowen.draw;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;

public class Quad extends Drawable {
    private static final float[] VERTICES = {
            // Positions        // Colors
            -0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f,  // Bottom-left, red
            0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,  // Bottom-right, green
            0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f,  // Top-right, blue
            -0.5f, 0.5f, 0.0f, 1.0f, 1.0f, 0.0f   // Top-left, yellow
    };

    private static final int[] INDICES = {
            0, 1, 2,  // First triangle
            2, 3, 0   // Second triangle
    };

    @Override
    public void draw() {
        vao.bind();
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        vao.unbind();
    }

    @Override
    protected void uploadVertexData() {
        vbo.bind(GL_ARRAY_BUFFER);
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(VERTICES.length).put(VERTICES).flip();
        vbo.uploadData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);
    }

    @Override
    protected void uploadIndexData() {
        ebo.bind(GL_ELEMENT_ARRAY_BUFFER);
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(INDICES.length).put(INDICES).flip();
        ebo.uploadData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);
    }

    @Override
    protected void setVertexAttributePointer() {
        // Set up vertex attribute pointers

        // Position attribute
        vbo.setVertexAttributePointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);

        // Color attribute
        vbo.setVertexAttributePointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
    }
}
