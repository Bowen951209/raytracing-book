package net.bowen.draw;

import net.bowen.system.VertexArrayObject;
import net.bowen.system.BufferObject;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;

public abstract class Drawable {
    protected final VertexArrayObject vao = new VertexArrayObject();
    protected final BufferObject vbo = new BufferObject();
    protected final BufferObject ebo = new BufferObject();
    private final float[] vertices;
    private final int[] indices;

    public Drawable(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;

        vao.bind();
        uploadVertexData();
        uploadIndexData();
        setVertexAttributePointer();
        vao.unbind();
    }

    protected void uploadVertexData() {
        vbo.bind(GL_ARRAY_BUFFER);
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length).put(vertices).flip();
        vbo.uploadData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);
    }
    protected void uploadIndexData() {
        ebo.bind(GL_ELEMENT_ARRAY_BUFFER);
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length).put(indices).flip();
        ebo.uploadData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indexBuffer);
    }

    public abstract void draw();

    protected abstract void setVertexAttributePointer();
}
