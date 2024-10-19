package net.bowen;

import net.bowen.gui.Window;

public class Main {
    public static void main(String[] args) {
        int sceneId = args.length == 0 ? 0 : Integer.parseInt(args[0]);
        new Window("Raytracing", 500, 300, sceneId);
    }
}