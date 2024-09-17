package net.bowen.system;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BufferObject extends Deleteable {
    private final int bufferId;

    public BufferObject() {
        super(true);
        bufferId = glGenBuffers();
    }

    @Override
    public void delete() {
        glDeleteBuffers(bufferId);
        System.out.println("Buffer object(" + bufferId + ") deleted.");
    }

    public int getId() {
        return bufferId;
    }

    public void bind(int target) {
        glBindBuffer(target, bufferId);
    }

    // Upload vertex data to the GPU
    public void uploadData(int target, FloatBuffer data, int usage) {
        glBufferData(target, data, usage);
    }

    // Upload index data to the GPU (for element array buffers)
    public void uploadData(int target, IntBuffer data, int usage) {
        glBufferData(target, data, usage);
    }

    // Define vertex attributes
    public void setVertexAttributePointer(int index, int size, int type, boolean normalized, int stride, int pointer) {
        glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        glEnableVertexAttribArray(index);
    }
}