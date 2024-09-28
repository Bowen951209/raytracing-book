package net.bowen.system;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

public class RaytraceExecutor {
    private final ShaderProgram program;
    private final int numGroupsX, numGroupsY;
    private final FloatBuffer lastColorScale = BufferUtils.createFloatBuffer(1);
    private final FloatBuffer thisColorScale = BufferUtils.createFloatBuffer(1);
    private final FloatBuffer randomFactor = BufferUtils.createFloatBuffer(1);
    private final List<Runnable> completeListeners = new ArrayList<>();

    /**
     * How many times we rendered.
     */
    private int renderIndex;
    private int samplePerPixel;
    private int startTime;
    private int finishPeriod = -1;
    private boolean isSampleComplete;

    public RaytraceExecutor(Texture quadTexture, ShaderProgram program) {
        this.program = program;

        // work group size
        int localSizeX = 16, localSizeY = 16; // this is set in the shader
        numGroupsX = (quadTexture.getWidth() + localSizeX - 1) / localSizeX;
        numGroupsY = (quadTexture.getHeight() + localSizeY - 1) / localSizeY;
    }

    public void setSamplePerPixel(int samplePerPixel) {
        this.samplePerPixel = samplePerPixel;
    }

    public void resetCompleteState() {
        isSampleComplete = false;
        renderIndex = 0;
        finishPeriod = -1;
    }

    public int getFinishPeriod() {
        return finishPeriod;
    }

    public void addCompleteListener(Runnable l) {
        completeListeners.add(l);
    }

    public void raytrace() {
        if (renderIndex == 0)
            startTime = (int) System.currentTimeMillis();

        // Put uniforms. The program will automatically use.
        thisColorScale.put(1f / (renderIndex + 1));
        lastColorScale.put(renderIndex == 0 ? 0 : 1f / renderIndex);
        randomFactor.put((float) Math.random());
        thisColorScale.flip();
        lastColorScale.flip();
        randomFactor.flip();
        program.setUniform1fv("this_color_scale", thisColorScale);
        program.setUniform1fv("last_color_scale", lastColorScale);
        program.setUniform1fv("u_rand_factor", randomFactor);

        glDispatchCompute(numGroupsX, numGroupsY, 1); // Dispatch the work groups

        // Ensure all work has completed
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT); // Ensure the write to image is visible to subsequent operations

        renderIndex++;
    }

    public boolean sampleComplete() {
        if (!isSampleComplete) {
            isSampleComplete = renderIndex >= samplePerPixel;

            // If turn from incomplete to complete, it's time to call the complete listeners.
            if (isSampleComplete) {
                finishPeriod = (int) System.currentTimeMillis() - startTime;
                completeListeners.forEach(Runnable::run);
            }
        }
        return isSampleComplete;
    }
}
