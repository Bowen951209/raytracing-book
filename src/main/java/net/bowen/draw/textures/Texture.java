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
    private static final int DEFAULT_TYPE_ID = 0;

    private int textureID;
    private int internalFormat;
    private int format;
    private int type;
    private int width, height;

    public Texture() {
        super(false);
    }

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
     * Get the packed value of the texture's information.
     * <p>
     *  - The upper 4 bits store the texture type.
     * </p>
     * <p>
     *  - The middle 16 bits store the index in {@link #TEXTURES_IN_COMPUTE}. It will then be the index to the texture array uniform in the compute shader.
     * </p>
     * <p>
     *  - The lower 16 bits store some detail float value. Some of the texture types use it.
     * </p>
     *
     * @return the id of the texture in {@link #TEXTURES_IN_COMPUTE} list.
     */
    public int getValue() {
        // Get the real values.
        int index = TEXTURES_IN_COMPUTE.indexOf(this);
        if (index == -1) {
            // Perlin noises are not in TEXTURES_IN_COMPUTE list because they're not stored in texture units, but they
            // have own index counter.
            if (this instanceof PerlinNoiseTexture noise)
                index = noise.index;
            else
                throw new IllegalStateException("Texture not found in TEXTURES list.");
        }

        int textureType = getTextureTypeId();

        float detail = getDetail();

        // Check validate input ranges.
        if (textureType < 0 || textureType > 15 || index > 65535 || detail < 0.0f || detail > 1.0f)
            throw new IllegalArgumentException("Input values out of range.");

        // Pack the values.
        // Convert detail float (0.0 to 1.0) into a 12-bit integer (0 to 4095)
        int detailBits = (int) (detail * 4095);  // Maps [0, 1] to [0, 4095]

        // Encoding:
        // - Shift textureTypeId to the upper 4 bits (28th to 31st bits)
        // - Shift index to the middle 16 bits (12th to 27th bits)
        // - Leave detail in the lower 12 bits (0th to 11th bits)
        return (textureType << 28) | (index << 12) | (detailBits & 0xFFF);
    }

    protected int getTextureTypeId() {
        return DEFAULT_TYPE_ID;
    }

    protected float getDetail() {
        return 0;
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
            active(TEXTURES_IN_COMPUTE.indexOf(texture));
            texture.bind();
        }
    }
}
