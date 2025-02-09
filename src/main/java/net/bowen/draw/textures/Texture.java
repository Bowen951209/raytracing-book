package net.bowen.draw.textures;

import net.bowen.system.Deleteable;
import net.bowen.system.ShaderProgram;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public class Texture extends Deleteable {
    public static final int DEFAULT = 0;
    public static final int IMAGE = 1;
    public static final int CHECKER = 2;
    public static final int PERLIN = 3;
    public static final int SOLID = 4;

    /**
     * The list of textures used in the raytrace compute shader.
     */
    public static final List<Texture> TEXTURES_IN_COMPUTE = new ArrayList<>();

    private final int textureID;
    private final int internalFormat;
    private final int format;
    private final int type;
    private int width = -1, height = -1;

    /**
     * Create a texture with initial data.
     *
     * @param width          the width of the texture
     * @param height         the height of the texture
     * @param internalFormat the format you're going to put in the buffer as.
     * @param format         the format in the shader.
     * @param type           the type of the data. e.g. {@link GL43#GL_UNSIGNED_BYTE}, {@link  GL43#GL_FLOAT}...
     * @param data           the initial data of the texture
     * @see GL43#glTexImage2D
     */
    public Texture(int width, int height, int internalFormat, int format, int type, ByteBuffer data) {
        this(internalFormat, format, type);
        this.width = width;
        this.height = height;

        putData(data);
    }

    /**
     * Create a texture with no initial data.
     *
     * @param internalFormat the format you're going to put in the buffer as.
     * @param format         the format in the shader.
     * @param type           the type of the data. e.g. {@link GL43#GL_UNSIGNED_BYTE}, {@link  GL43#GL_FLOAT}...
     */
    public Texture(int internalFormat, int format, int type) {
        super(true);
        this.internalFormat = internalFormat;
        this.format = format;
        this.type = type;
        textureID = glGenTextures();

        // Bind the texture to set its parameters
        bind();

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

    public void saveAsPNG(String filename) {
        bind();
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 3); // Assuming 3 bytes per pixel (RGB)
        try {
            glGetTexImage(GL_TEXTURE_2D, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer);

            // Apply gamma correction (2.2)
            for (int i = 0; i < buffer.capacity(); i++) {
                float corrected = (float) Math.pow((buffer.get(i) & 0xFF) / 255.0, 1.0 / 2.2) * 255.0f;
                buffer.put(i, (byte) corrected);
            }

            // Use Java's ImageIO to write the image
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (x + (width * y)) * 3;
                    int r = buffer.get(index) & 0xFF;
                    int g = buffer.get(index + 1) & 0xFF;
                    int b = buffer.get(index + 2) & 0xFF;
                    int rgb = (r << 16) | (g << 8) | b;
                    image.setRGB(x, y, rgb);
                }
            }
            File outputFile = new File(filename);
            ImageIO.write(image, "png", outputFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save texture as PNG", e);
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    public void putData(ByteBuffer data) {
        if (width == -1 || height == -1)
            throw new IllegalStateException("Texture size not set.");

        bind();
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, data);
    }

    public void putData(int width, int height, ByteBuffer data) {
        bind();
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, data);
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
     * - The upper 4 bits store the texture type.
     * </p>
     * <p>
     * - The middle 16 bits store the index in {@link #TEXTURES_IN_COMPUTE}. It will then be the index to the texture array uniform in the compute shader.
     * </p>
     * <p>
     * - The lower 16 bits store some detail float value. Some of the texture types use it.
     * </p>
     *
     * @return the id of the texture in {@link #TEXTURES_IN_COMPUTE} list.
     */
    public int getValue() {
        // Get the real values.
        int index = TEXTURES_IN_COMPUTE.indexOf(this);
        if (index == -1)
            throw new IllegalStateException("Texture not found in TEXTURES list.");

        int textureType = getTextureTypeId();
        float detail = getDetail();

        return getValue(textureType, index, detail);
    }

    protected int getTextureTypeId() {
        return DEFAULT;
    }

    protected float getDetail() {
        return 0;
    }

    public static void active(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
    }

    public static int getValue(int textureType, int index, float detail) {
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

    public static int getValue(int textureType, int index, int detail) {
        // Check validate input ranges.
        if (textureType < 0 || textureType > 15 || index > 65535 || detail < -2048 || detail > 2047)
            throw new IllegalArgumentException("Input values out of range.");

        // Encoding:
        // - Shift textureTypeId to the upper 4 bits (28th to 31st bits)
        // - Shift index to the middle 16 bits (12th to 27th bits)
        // - Leave detail in the lower 12 bits (0th to 11th bits)
        return (textureType << 28) | (index << 12) | (detail & 0xFFF);
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
