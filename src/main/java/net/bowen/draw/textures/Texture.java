package net.bowen.draw.textures;

import net.bowen.system.Deleteable;
import net.bowen.system.ShaderProgram;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public class Texture extends Deleteable {
    /**
     * The list of textures used in the raytrace compute shader.
     */
    private static final List<Texture> TEXTURES_IN_COMPUTE = new ArrayList<>();

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

    /**
     * Get the information of the texture. Generally, only integer digits are used, but sometimes, like checkerboard,
     * store its detail value (in which case is scale), in the float digits.
     *
     * @return the id of the texture in {@link #TEXTURES_IN_COMPUTE} list.
     */
    public float getValue() {
        int index = TEXTURES_IN_COMPUTE.indexOf(this);
        if (index == -1)
            throw new IllegalStateException("Texture not found in TEXTURES list.");
        return index;
    }

    public static void active(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
    }


    /**
     * Add texture to {@link #TEXTURES_IN_COMPUTE}, the list which stores the textures used in the compute shader.
     */
    public static void texturesInComputeAdd(Texture texture) {
        TEXTURES_IN_COMPUTE.add(texture);
    }

    public static void putTextureIndices(ShaderProgram program) {
        IntBuffer buffer = MemoryUtil.memAllocInt(TEXTURES_IN_COMPUTE.size());
        for (int i = 0; i < TEXTURES_IN_COMPUTE.size(); i++)
            buffer.put(i);

        buffer.flip();

        program.setUniform1iv("textures", buffer);
        MemoryUtil.memFree(buffer);
    }

    public static void bindTexturesInCompute() {
        for (Texture texture : TEXTURES_IN_COMPUTE) {
            active((int) texture.getValue());
            texture.bind();
        }
    }
}
