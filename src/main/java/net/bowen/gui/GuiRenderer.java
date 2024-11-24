package net.bowen.gui;

import imgui.ImGui;
import net.bowen.system.RaytraceExecutor;

public class GuiRenderer implements GuiLayer {
    private final Window window;
    private final RaytraceExecutor raytraceExecutor;
    private final int[] samplePerPixel = new int[] {20};
    private final int[] maxDepth = new int[] {5};

    public GuiRenderer(Window window) {
        this.window = window;
        raytraceExecutor = window.getRaytraceExecutor();

        multiSampleSliderSlide();
        maxBounceSliderSlide();
    }

    @Override
    public void draw() {
        int dispatchTime = raytraceExecutor.getLastDispatchTime();
        int finishTime = raytraceExecutor.getFinishTime();
        int numSample = raytraceExecutor.getNumSamples(); // num of samples that were taken.
        int numAllSample = raytraceExecutor.getSamplePerPixel(); // num of samples that should be taken.

        String dispatchInfoText = "Last raytrace took: " + dispatchTime + " ms.";
        ImGui.text(dispatchInfoText);

        String sampleInfoText = "Sample: " + numSample + "/" + numAllSample + ".";

        // If all samples are finished, add more text.
        if (finishTime != -1) {
            sampleInfoText += "Render completed in: " + finishTime + " ms.";
        }
        ImGui.text(sampleInfoText);

        // Slider for multi-sample count.
        if (ImGui.sliderInt("Sample per pixel", samplePerPixel, 1, 100)) {
            multiSampleSliderSlide();
        }

        // Slider for max bounces of ray.
        if (ImGui.sliderInt("Max Depth", maxDepth, 1, 50)) {
            maxBounceSliderSlide();
        }
    }

    private void multiSampleSliderSlide() {
        raytraceExecutor.setSamplePerPixel(samplePerPixel[0]);
        raytraceExecutor.resetCompleteState();
    }

    public void maxBounceSliderSlide() {
        // Send the count to the shader.
        window.computeProgram.setUniform1iv("max_depth", maxDepth);
        raytraceExecutor.resetCompleteState();
    }
}
