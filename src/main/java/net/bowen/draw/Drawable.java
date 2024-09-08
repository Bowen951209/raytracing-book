package net.bowen.draw;

import net.bowen.system.VertexArrayObject;
import net.bowen.system.BufferObject;

public abstract class Drawable {
    protected final VertexArrayObject vao = new VertexArrayObject();
    protected final BufferObject vbo = new BufferObject();
    protected final BufferObject ebo = new BufferObject();

    public Drawable() {
        vao.bind();
        uploadVertexData();
        uploadIndexData();
        setVertexAttributePointer();
        vao.unbind();
    }

    public abstract void draw();

    protected abstract void uploadVertexData();
    protected abstract void uploadIndexData();
    protected abstract void setVertexAttributePointer();
}
