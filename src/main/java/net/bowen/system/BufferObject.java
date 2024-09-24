package net.bowen.system;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BufferObject extends Deleteable {
    private final int bufferId;
    private final int bufferType;

    public BufferObject(int bufferType) {
        super(true);
        this.bufferType = bufferType;
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

    public void bind() {
        glBindBuffer(bufferType, bufferId);
    }

    // Upload vertex data to the GPU
    public void uploadData(FloatBuffer data, int usage) {
        glBufferData(bufferType, data, usage);
    }

    // Upload index data to the GPU (for element array buffers)
    public void uploadData(IntBuffer data, int usage) {
        glBufferData(bufferType, data, usage);
    }

    // Define vertex attributes
    public void setVertexAttributePointer(int index, int size, int type, boolean normalized, int stride, int pointer) {
        glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        glEnableVertexAttribArray(index);
    }
}