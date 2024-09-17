package net.bowen.gui;

import imgui.ImGui;
import net.bowen.system.QueryTimer;

public class GuiRenderer implements GuiLayer {
    private final Window window;
    private final QueryTimer queryTimer = new QueryTimer();

    public GuiRenderer(Window window) {
        this.window = window;
    }

    @Override
    public void draw() {
        if (ImGui.button("render")) {
            renderBtnClicked();
        }

        if (queryTimer.checkResultAvailable()) {
            int timeMs = queryTimer.getElapsedTime();
            ImGui.text("Rendered in: " + timeMs + " ms.");
        } else {
            ImGui.text("Rendering in progress...");
        }
    }

    public void renderBtnClicked() {
        queryTimer.startQuery();
        window.raytrace();
        queryTimer.endQuery();
    }
}
