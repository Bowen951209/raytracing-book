package net.bowen;

import imgui.ImGui;
import net.bowen.gui.GuiLayer;
import net.bowen.gui.Window;

public class Main {
    public static void main(String[] args) {
        GuiLayer guiLayer = ImGui::showDemoWindow;
        new Window("Ray tracing", guiLayer);
    }
}