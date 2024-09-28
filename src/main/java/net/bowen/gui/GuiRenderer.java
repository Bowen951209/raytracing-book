package net.bowen.gui;

import imgui.ImGui;
import net.bowen.system.QueryTimer;

public class GuiRenderer implements GuiLayer {
    private final Window window;
    private final QueryTimer queryTimer = new QueryTimer();
    private final int[] samplePerPixel = new int[] {100};
    private final int[] maxDepth = new int[] {50};

    public GuiRenderer(Window window) {
        this.window = window;
    }

    @Override
    public void draw() {
        // The render button.
        if (ImGui.button("render")) {
            renderBtnClicked();
        }

        // The text viewing the elapsed time of the raytrace.
        ImGui.sameLine();
        if (queryTimer.checkResultAvailable()) {
            int timeMs = queryTimer.getElapsedTime();
            ImGui.text("Rendered in: " + timeMs + " ms.");
        } else {
            ImGui.text("Rendering in progress...");
        }

        // Slider for multi-sample count.
        if (ImGui.sliderInt("Sample per pixel", samplePerPixel, 1, 100)) {
            multiSampleSliderSlide();
        }

        // Slider for max bounces of ray.
        if (ImGui.sliderInt("Max Depth", maxDepth, 1, 50)) {
            maxBounceSliderSlide();
        }
    }

    public void renderBtnClicked() {
        queryTimer.startQuery();
        window.raytrace();
        queryTimer.endQuery();
    }

    public void multiSampleSliderSlide() {
        // Send the count to the shader.
        window.computeProgram.setUniform1iv("sample_per_pixel", samplePerPixel);
    }

    public void maxBounceSliderSlide() {
        // Send the count to the shader.
        window.computeProgram.setUniform1iv("max_depth", maxDepth);
    }
}
