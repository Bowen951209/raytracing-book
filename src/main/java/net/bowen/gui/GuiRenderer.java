package net.bowen.gui;

import imgui.ImGui;
import net.bowen.system.RaytraceExecutor;

public class GuiRenderer implements GuiLayer {
    private final Window window;
    private final RaytraceExecutor raytraceExecutor;
    private final int[] samplePerPixel = new int[1];
    private final int[] maxDepth = new int[1];

    private String savedImagePath;

    public GuiRenderer(Window window) {
        this.window = window;
        raytraceExecutor = window.getRaytraceExecutor();

        samplePerPixelUpdate();
        maxDepthUpdate();
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
        if (ImGui.sliderInt("Sample per pixel", samplePerPixel, 1, 10_000)) {
            samplePerPixelUpdate();
        }

        // Slider for max bounces of ray.
        if (ImGui.sliderInt("Max Depth", maxDepth, 1, 50)) {
            maxDepthUpdate();
        }

        // Save image button.
        if (ImGui.button("Save Image")) {
            savedImagePath = window.saveImage("image.png");
        }
        if (savedImagePath != null) {
            ImGui.text("Image saved to: " + savedImagePath);
        }
    }

    private void samplePerPixelUpdate() {
        raytraceExecutor.setSamplePerPixel(samplePerPixel[0]);
        raytraceExecutor.resetCompleteState();
    }

    public void maxDepthUpdate() {
        // Send the count to the shader.
        window.computeProgram.setUniform1iv("max_depth", maxDepth);
        raytraceExecutor.resetCompleteState();
    }

    public void setSamplePerPixel(int samplePerPixel) {
        this.samplePerPixel[0] = samplePerPixel;
        samplePerPixelUpdate();
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth[0] = maxDepth;
        maxDepthUpdate();
    }
}
