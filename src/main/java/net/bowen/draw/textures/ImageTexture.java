package net.bowen.draw.textures;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.Objects;

import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.stb.STBImage.*;

public class ImageTexture extends Texture {
    public ImageTexture(int width, int height, int internalFormat, int format, int type, ByteBuffer data) {
        super(width, height, internalFormat, format, type, data);
    }

    public static ImageTexture create(String resourcePath) {
        return create(resourcePath, 0, 0);
    }

    public static ImageTexture create(String resourcePath, int shiftX, int shiftY) {
        URL resource = Objects.requireNonNull(Texture.class.getClassLoader().getResource(resourcePath));
        String filePath;
        try {
            filePath = String.valueOf(Paths.get(resource.toURI()).toFile());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Initialize LWJGL's memory stack management
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Allocate variables to store the image's width, height, and channel count
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load the image using STBImage
            stbi_set_flip_vertically_on_load(true); // Flip the image vertically for correct OpenGL texture orientation

            ByteBuffer image = stbi_load(filePath, width, height, channels, 0);

            // Check if the image was successfully loaded
            if (image == null)
                throw new RuntimeException("Failed to load image: " + stbi_failure_reason());

            // Retrieve the width, height, and channel count of the image
            int imgWidth = width.get();
            int imgHeight = height.get();
            int imgChannels = channels.get();

            System.out.println("Image loaded: " + filePath);
            System.out.println("Width: " + imgWidth + ", Height: " + imgHeight + ", Channels: " + imgChannels);

            // Determine the image format based on the number of channels
            int format;
            if (imgChannels == 3) {
                format = GL_RGB;
            } else if (imgChannels == 4) {
                format = GL_RGBA;
            } else {
                // If the image format is unsupported, throw an error
                throw new RuntimeException("Unsupported image format");
            }

            // Create a buffer for the shifted image
            ByteBuffer shiftedBuffer = BufferUtils.createByteBuffer(image.capacity());

            for (int y = 0; y < imgHeight; y++) {
                // Calculate the source row with wrapping for vertical shift
                int sourceY = (y - shiftY + imgHeight) % imgHeight;

                for (int x = 0; x < imgWidth; x++) {
                    // Calculate the source column with wrapping for horizontal shift
                    int sourceX = (x - shiftX + imgWidth) % imgWidth;

                    // Calculate source and destination indices in the 1D buffer
                    int srcIndex = (sourceY * imgWidth + sourceX) * imgChannels; // Source pixel
                    int dstIndex = (y * imgWidth + x) * imgChannels;             // Destination pixel


                    // Copy pixel data
                    for (int i = 0; i < imgChannels; i++) {
                        shiftedBuffer.put(dstIndex + i, image.get(srcIndex + i));
                    }
                }
            }

            // Return a new Texture object with the image's data
            ImageTexture instance = new ImageTexture(imgWidth, imgHeight, format, format, GL_UNSIGNED_BYTE, shiftedBuffer);
            texturesInComputeAdd(instance);

            // Free the image memory once loaded
            stbi_image_free(image);

            return instance;
        }
    }

    @Override
    protected int getTextureTypeId() {
        return IMAGE;
    }
}
