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
    }

    @Override
    public void draw() {
        int renderCompleteTime = raytraceExecutor.getFinishPeriod();
        String text = renderCompleteTime == -1 ? "Rendering..." : "Rendered in: " + renderCompleteTime + " ms.";
        ImGui.text(text);

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
