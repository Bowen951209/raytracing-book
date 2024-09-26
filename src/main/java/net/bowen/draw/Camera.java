package net.bowen.draw;

import net.bowen.system.BufferObject;
import net.bowen.system.ShaderProgram;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL43.*;

public class Camera {
    private int imageWidth, imageHeight;
    private float centerX = 0, centerY = 0, centerZ = 0;
    private float vFOV = 90; // vertical view angle
    private float aspectRatio;
    private ShaderProgram program;
    private BufferObject ssbo;

    public void init() {
        // Determine viewport dimensions.
        float focalLength = 1.0f;
        float theta = (float) Math.toRadians(vFOV);
        float h = (float) (Math.tan(theta / 2.0f));
        float viewportHeight =  2.0f * h * focalLength;
        float viewportWidth = viewportHeight * aspectRatio;

        // Calculate the horizontal and vertical delta vectors from pixel to pixel.
        float[] pixelDeltaU = {viewportWidth / imageWidth, 0.0f};
        float[] pixelDeltaV = {0.0f, -viewportHeight / imageHeight};

        // Init the SSBO.
        ssbo = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        ssbo.bind();
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, ssbo.getId());

        putToShaderProgram(viewportWidth, viewportHeight, pixelDeltaU, pixelDeltaV);
    }

    private void putToShaderProgram(float viewportWidth, float viewportHeight, float[] pixelDeltaU, float[] pixelDeltaV) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(8);
        buffer.put(viewportWidth).put(viewportHeight).put(aspectRatio);
        buffer.put(0);
        buffer.put(pixelDeltaU);
        buffer.put(pixelDeltaV);
        buffer.flip();
        ssbo.uploadData(buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
    }

    /**
     * Specify the size of the image we're rendering to. This is needed for calculations in initialization.
     */
    public void setImageSize(int w, int h) {
        imageWidth = w;
        imageHeight = h;
        aspectRatio = (float) w / h;
    }

    public void setPosition(float x, float y, float z) {
        centerX = x;
        centerY = y;
        centerZ = z;
    }

    public void setVerticalFOV(float vFOV) {
        this.vFOV = vFOV;
    }

    /**
     * Set the target shader program we want to put the camera's properties to.
     */
    public void setProgram(ShaderProgram program) {
        this.program = program;
    }
}
