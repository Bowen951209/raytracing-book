package net.bowen.system;

import static org.lwjgl.opengl.GL30.*;

public class VertexArrayObject extends Deleteable {
    private final int vaoId;

    public VertexArrayObject() {
        super(true);
        vaoId = glGenVertexArrays();
    }

    @Override
    public void delete() {
        glDeleteVertexArrays(vaoId);
        System.out.println("VAO(" + vaoId + ") deleted.");
    }

    // Bind VAO
    public void bind() {
        glBindVertexArray(vaoId);
    }

    // Unbind VAO
    public void unbind() {
        glBindVertexArray(0);
    }
}
