package net.bowen.draw.textures;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL43.*;

public class ImageTexture extends Texture {
    public ImageTexture(int width, int height, int internalFormat, int format, int type, ByteBuffer data) {
        super(width, height, internalFormat, format, type, data);
    }

    public static ImageTexture create(String resourcePath) {
        return create(resourcePath, 0, 0);
    }

    public static ImageTexture create(String resourcePath, int shiftX, int shiftY) {
        try {
            // Load the image using ImageIO
            InputStream imageInputStream = ImageTexture.class.getClassLoader().getResourceAsStream(resourcePath);
            assert imageInputStream != null;
            BufferedImage bufferedImage = ImageIO.read(imageInputStream);

            if (bufferedImage == null) {
                throw new RuntimeException("Failed to load image: " + resourcePath);
            }

            int imgWidth = bufferedImage.getWidth();
            int imgHeight = bufferedImage.getHeight();
            int imgChannels = bufferedImage.getColorModel().getNumComponents();

            System.out.println("Image loaded: " + resourcePath);
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
            ByteBuffer shiftedBuffer = BufferUtils.createByteBuffer(imgWidth * imgHeight * imgChannels);

            for (int y = 0; y < imgHeight; y++) {
                // Calculate the source row with wrapping for vertical shift and flip vertically
                int sourceY = (imgHeight - 1 - (y - shiftY + imgHeight) % imgHeight);

                for (int x = 0; x < imgWidth; x++) {
                    // Calculate the source column with wrapping for horizontal shift
                    int sourceX = (x - shiftX + imgWidth) % imgWidth;

                    // Get the pixel data
                    int pixel = bufferedImage.getRGB(sourceX, sourceY);

                    // Extract the color components
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    int a = (pixel >> 24) & 0xFF;

                    // Calculate destination index in the 1D buffer
                    int dstIndex = (y * imgWidth + x) * imgChannels;

                    // Copy pixel data
                    shiftedBuffer.put(dstIndex, (byte) r);
                    shiftedBuffer.put(dstIndex + 1, (byte) g);
                    shiftedBuffer.put(dstIndex + 2, (byte) b);
                    if (imgChannels == 4) {
                        shiftedBuffer.put(dstIndex + 3, (byte) a);
                    }
                }
            }

            // Return a new Texture object with the image's data
            ImageTexture instance = new ImageTexture(imgWidth, imgHeight, format, format, GL_UNSIGNED_BYTE, shiftedBuffer);
            texturesInComputeAdd(instance);

            return instance;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image: " + resourcePath, e);
        }
    }

    @Override
    protected int getTextureTypeId() {
        return IMAGE;
    }
}
