package net.bowen.gui;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.bowen.draw.Quad;
import net.bowen.system.Deleteable;
import net.bowen.system.Shader;
import net.bowen.system.ShaderProgram;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private final String title;
    private final GuiLayer guiLayer;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private long windowHandle;
    private Quad screenQuad;

    public Window(String title, GuiLayer guiLayer) {
        this.title = title;
        this.guiLayer = guiLayer;

        System.out.println("LWJGL version: " + Version.getVersion());

        init();
        loop();
        free();
    }

    private void init() {
        initGLFW();
        initImGui();
        initShaderPrograms();
        initModels();

        // Make the window visible
        glfwShowWindow(windowHandle);
    }

    private void initGLFW() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        windowHandle = glfwCreateWindow(300, 300, title, NULL, NULL);
        if (windowHandle == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowHandle, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            assert vidmode != null;
            glfwSetWindowPos(
                    windowHandle,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);
        // Enable v-sync
        glfwSwapInterval(1);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
    }

    private void initImGui() {
        ImGui.createContext();
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init("#version 430 core");
    }

    private void initShaderPrograms() {
        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.attachShader(new Shader("shaders/simpleShaders/vert.glsl", GL_VERTEX_SHADER));
        shaderProgram.attachShader(new Shader("shaders/simpleShaders/frag.glsl", GL_FRAGMENT_SHADER));
        shaderProgram.link();
        shaderProgram.use();
    }

    private void initModels() {
        screenQuad = new Quad();
    }

    private void drawModels() {
        screenQuad.draw();
    }

    private void loop() {
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(windowHandle)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            imGuiGlfw.newFrame();
            imGuiGl3.newFrame();
            ImGui.newFrame();

            drawModels();

            guiLayer.draw();

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            // Multi Viewports things.
            final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);

            glfwSwapBuffers(windowHandle); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    private void free() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(windowHandle);
        System.out.println("GLFW callbacks freed.");

        //noinspection DataFlowIssue
        glfwSetErrorCallback(null).free();
        System.out.println("GLFW error callback freed.");

        glfwDestroyWindow(windowHandle);
        System.out.println("GLFW window(" + windowHandle + ") destroyed.");

        // Terminate GLFW and free the error callback
        glfwTerminate();
        System.out.println("GLFW terminated.");


        // Free ImGUI
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
        System.out.println("ImGUI freed.");

        // Free the deletables
        Deleteable.deleteCreatedInstances();
    }
}
