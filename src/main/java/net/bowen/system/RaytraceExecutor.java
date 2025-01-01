package net.bowen.system;

import net.bowen.draw.textures.Texture;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

public class RaytraceExecutor {
    private final Texture quadTexture;
    private final ShaderProgram program;
    private final FloatBuffer randomFactor = BufferUtils.createFloatBuffer(1);
    private final List<Runnable> completeListeners = new ArrayList<>();
    private final List<QueryTimer> timers = new ArrayList<>();

    /**
     * How many samples have been taken. It's added 1 per dispatch call.
     */
    private int numSamples;
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
        this.quadTexture = quadTexture;
        this.program = program;
    }

    public void setSamplePerPixel(int samplePerPixel) {
        this.samplePerPixel = samplePerPixel;

        float sqrtSpp = (float) Math.sqrt(samplePerPixel);
        program.setUniform1f("sqrt_spp", sqrtSpp);
        program.setUniform1f("recip_sqrt_spp", 1f / sqrtSpp);
    }

    public void resetCompleteState() {
        isSampleComplete = false;
        numSamples = 0;
        finishTime = -1;
    }

    public int getNumSamples() {
        return numSamples;
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
        Texture.bindTexturesInCompute();
        // Set the start time if it's the very first raytrace.
        if (numSamples == 0)
            startTime = (int) System.currentTimeMillis();

        // Check available timers and put theirs value in to #lastDispatchTime.
        Iterator<QueryTimer> iterator = timers.iterator();
        while (iterator.hasNext()) {
            QueryTimer timer = iterator.next();
            if (timer.checkResultAvailable()) {
                lastDispatchTime = timer.getElapsedTime();
                timer.delete();
                iterator.remove();  // Safely remove the element
            }
        }

        // Start timer for single trace timing.
        QueryTimer timer = new QueryTimer();
        timer.setSilent();
        timers.add(timer);
        timer.startQuery();

        // Put uniforms. The program will automatically use.
        randomFactor.put((float) Math.random());
        randomFactor.flip();
        program.setUniform1i("frame_count", ++numSamples);
        program.setUniform1fv("u_rand_factor", randomFactor);

        // Work group size.
        int localSizeX = 16, localSizeY = 16; // this is set in the shader
        int numGroupsX = (quadTexture.getWidth() + localSizeX - 1) / localSizeX;
        int numGroupsY = (quadTexture.getHeight() + localSizeY - 1) / localSizeY;

        // The dispatch call. This takes most of the time.
        glDispatchCompute(numGroupsX, numGroupsY, 1); // Dispatch the work groups

        // Ensure all work has completed.
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT); // Ensure the write to image is visible to subsequent operations

        // End timer.
        timer.endQuery();
    }

    public boolean sampleComplete() {
        if (!isSampleComplete) {
            isSampleComplete = numSamples >= samplePerPixel;

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
