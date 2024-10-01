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
    private final List<QueryTimer> timers = new ArrayList<>();

    /**
     * How many samples have been taken. It's added 1 per dispatch call.
     */
    private int samples;
    /**
     * The system time of the first raytrace.
     */
    private int startTime;
    /**
     * How long it took for all samples to finish.
     */
    private int finishTime = -1;
    /**
     * How long it took for 1 dispatch call.
     */
    private int lastDispatchTime;
    /**
     * If all samples are finished.
     */
    private boolean isSampleComplete;
    private int samplePerPixel;

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
        samples = 0;
        finishTime = -1;
    }

    public int getSamples() {
        return samples;
    }

    public int getSamplePerPixel() {
        return samplePerPixel;
    }

    public int getFinishTime() {
        return finishTime;
    }

    public int getLastDispatchTime() {
        return lastDispatchTime;
    }


    public void addCompleteListener(Runnable l) {
        completeListeners.add(l);
    }

    public void raytrace() {
        // Set the start time if it's the very first raytrace.
        if (samples == 0)
            startTime = (int) System.currentTimeMillis();

        // Check available timers and put theirs value in to #lastDispatchTime.
        for (QueryTimer timer : timers) {
            if (timer.checkResultAvailable()) {
                lastDispatchTime = timer.getElapsedTime();
                timer.delete();
                timers.remove(timer);
            }
        }

        // Start timer for single trace timing.
        QueryTimer timer = new QueryTimer();
        timer.setSilent();
        timers.add(timer);
        timer.startQuery();

        // Put uniforms. The program will automatically use.
        thisColorScale.put(1f / (samples + 1));
        lastColorScale.put(samples == 0 ? 0 : 1f / samples);
        randomFactor.put((float) Math.random());
        thisColorScale.flip();
        lastColorScale.flip();
        randomFactor.flip();
        program.setUniform1fv("this_color_scale", thisColorScale);
        program.setUniform1fv("last_color_scale", lastColorScale);
        program.setUniform1fv("u_rand_factor", randomFactor);

        // The dispatch call. This takes most of the time.
        glDispatchCompute(numGroupsX, numGroupsY, 1); // Dispatch the work groups

        // Ensure all work has completed.
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT); // Ensure the write to image is visible to subsequent operations

        // End timer.
        timer.endQuery();

        samples++;
    }

    public boolean sampleComplete() {
        if (!isSampleComplete) {
            isSampleComplete = samples >= samplePerPixel;

            // If turn from incomplete to complete, it's time to call the complete listeners.
            if (isSampleComplete) {
                // Set the finish time.
                finishTime = (int) System.currentTimeMillis() - startTime;
                completeListeners.forEach(Runnable::run);
            }
        }
        return isSampleComplete;
    }
}
