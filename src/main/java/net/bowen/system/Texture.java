package net.bowen.system;

import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;

public class Texture extends Deleteable{
    private final int textureID;
    private final int internalFormat;
    private final int format;
    private final int type;

    private int width, height;

    public Texture(int width, int height, int internalFormat, int format, int type, ByteBuffer data) {
        super(true);
        this.width = width;
        this.height = height;
        this.internalFormat = internalFormat;
        this.format = format;
        this.type = type;
        textureID = glGenTextures();

        // Bind the texture to set its parameters
        bind();

        // Upload the texture data
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, data);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Unbind the texture
        unbind();
    }

    @Override
    public void delete() {
        glDeleteTextures(textureID);
        System.out.println("Texture(" + textureID + ") deleted.");
    }

    public void resize(int width, int height) {
        bind();
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
        this.width = width;
        this.height = height;
    }

    // Binds this texture to the active texture unit
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    // Unbinds any texture currently bound
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // Binds this texture as an image texture for compute shaders
    public void bindAsImage(int unit, int access, int internalFormat) {
        glBindImageTexture(unit, textureID, 0, false, 0, access, internalFormat);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static void active(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
    }
}
