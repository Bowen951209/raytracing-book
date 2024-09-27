package net.bowen.draw;

import net.bowen.system.BufferObject;
import net.bowen.system.DataUtils;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL43.*;

public class Camera {
    /**
     * The point camera is looking from.
     */
    private final Vector3f lookFrom = new Vector3f(0, 0, 0);
    /**
     * The point camera is looking at.
     */
    private final Vector3f lookAt = new Vector3f(0, 0, -1);
    /**
     * The up direction of the camera.
     */
    private final Vector3f vUp = new Vector3f(0, 1, 0);
    /**
     * The unit basis vector for the camera coordinate frame.
     */
    private final Vector3f u = new Vector3f(), v = new Vector3f(), w = new Vector3f();
    /**
     * The delta for a single pixel. (From pixel step to normal coordinate step)
     */
    private final Vector3f pixelDeltaU = new Vector3f(), pixelDeltaV = new Vector3f();
    /**
     * The up left position for the viewport in normal coordinate.
     */
    private final Vector3f upLeftPosition = new Vector3f();


    /**
     * The size of the image/texture we're rendering to.
     */
    private int imageWidth, imageHeight;
    /**
     * The size of the viewport. (size of the normal coordinate system)
     */
    private float viewportWidth, viewportHeight;
    /**
     * The vertical field of view in degree.
     */
    private float vFOV = 90;
    /**
     * The value of {@link #imageWidth} / {@link #imageHeight}
     */
    private float aspectRatio;
    /**
     * The shader storage buffer object which is bind to the compute shader's binding point.
     */
    private BufferObject ssbo;

    /**
     * Init the camera and send the set data to the specified shader program.
     */
    public void init() {
        // Determine viewport dimensions.
        float focalLength = new Vector3f(lookFrom).sub(lookAt).length();
        float theta = (float) Math.toRadians(vFOV);
        float h = (float) (Math.tan(theta / 2.0f));
        viewportHeight = 2.0f * h * focalLength;
        viewportWidth = viewportHeight * aspectRatio;

        // Calculate the u, v, w.
        w.set(lookFrom).sub(lookAt).normalize();
        u.set(vUp).cross(w);
        v.set(w).cross(u);

        Vector3f viewportU = new Vector3f(u).mul(viewportWidth);
        Vector3f viewportV = new Vector3f(v).mul(-viewportHeight);

        // Calculate the horizontal and vertical delta vectors from pixel to pixel.
        pixelDeltaU.set(viewportU).div(imageWidth);
        pixelDeltaV.set(viewportV).div(imageHeight);

        // Up left position.
        upLeftPosition.set(lookAt);
        upLeftPosition.sub(new Vector3f(viewportU).div(2)); // translate left
        upLeftPosition.sub(new Vector3f(viewportV).div(2)); // translate up v it up


        // Init the SSBO.
        ssbo = new BufferObject(GL_SHADER_STORAGE_BUFFER);
        ssbo.bind();
        // Bind the SSBO to a binding point
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, ssbo.getId());

        putToShaderProgram();
    }

    private void putToShaderProgram() {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(20);
        buffer.put(viewportWidth).put(viewportHeight).put(aspectRatio);
        buffer.put(0);
        DataUtils.putToBuffer(lookFrom, buffer);
        buffer.put(0);
        DataUtils.putToBuffer(upLeftPosition, buffer);
        buffer.put(0);
        DataUtils.putToBuffer(pixelDeltaU, buffer);
        buffer.put(0);
        DataUtils.putToBuffer(pixelDeltaV, buffer);
        buffer.put(0);
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

    public void setLookFrom(float x, float y, float z) {
        lookFrom.set(x, y, z);
    }

    public void setLookAt(float x, float y, float z) {
        lookAt.set(x, y, z);
    }

    public void setVerticalFOV(float vFOV) {
        this.vFOV = vFOV;
    }
}
