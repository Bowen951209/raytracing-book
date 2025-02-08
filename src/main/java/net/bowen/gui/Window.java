package net.bowen.gui;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.bowen.draw.Scene;
import net.bowen.draw.models.rasterization.Quad;
import net.bowen.draw.textures.Texture;
import net.bowen.system.Deleteable;
import net.bowen.system.RaytraceExecutor;
import net.bowen.system.Shader;
import net.bowen.system.ShaderProgram;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private final String title;
    private final String outputFile;
    private final int sceneId;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private long windowHandle;
    private int width, height;
    private Quad screenQuad;
    private Texture screenQuadTexture;
    private GuiRenderer guiRenderer;
    private RaytraceExecutor raytraceExecutor;
    private Scene scene;

    ShaderProgram screenQuadProgram, computeProgram;

    public Window(String title, int sceneId, int width, int height, int samplePerPixel, int maxDepth, String outputFile) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.sceneId = sceneId;
        this.outputFile = outputFile;

        System.out.println("LWJGL version: " + Version.getVersion());

        init();
        guiRenderer.setSamplePerPixel(samplePerPixel);
        guiRenderer.setMaxDepth(maxDepth);

        loop();
        free();
    }

    private void init() {
        System.out.println("Initializing...");
        long startTime = System.currentTimeMillis();

        System.out.println("Working directory: " + System.getProperty("user.dir"));

        initGLFW();
        initShaderPrograms();
        initScreenQuad();
        initModels();
        initRaytraceExecutor();
        initImGui();

        float initTime = (System.currentTimeMillis() - startTime) / 1000f;
        System.out.println("Initialization completed in " + initTime + " sec.");

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

        // Create the window
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Frame buffer resize callback
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> {
            this.width = width;
            this.height = height;

            // Resize GL viewport.
            glViewport(0, 0, width, height);

            // Resize textures and camera.
            screenQuadTexture.resize(width, height);
            scene.updateCamera(width, height);

            // Reset raytrace state.
            raytraceExecutor.resetCompleteState();
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

        // Gamma correction (gamma2.2)
        glEnable(GL_FRAMEBUFFER_SRGB);
    }

    private void initImGui() {
        ImGui.createContext();
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init("#version 430 core");

        // Set the scale
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        AffineTransform transform = gc.getDefaultTransform();
        float scale = (float) transform.getScaleX();  // Usually X is equal to Y, so take X here.
        ImGui.getStyle().scaleAllSizes(scale);
        ImGui.getIO().setFontGlobalScale(scale);

        guiRenderer = new GuiRenderer(this);
    }

    private void initShaderPrograms() {
        screenQuadProgram = new ShaderProgram();
        screenQuadProgram.attachShader(new Shader("shaders/plainTextureShaders/vert.glsl", GL_VERTEX_SHADER));
        screenQuadProgram.attachShader(new Shader("shaders/plainTextureShaders/frag.glsl", GL_FRAGMENT_SHADER));
        screenQuadProgram.link();

        computeProgram = new ShaderProgram();
        computeProgram.attachShader(new Shader("shaders/raytrace/compute.glsl", GL_COMPUTE_SHADER));
        computeProgram.link();
    }

    private void initModels() {
        scene = new Scene(sceneId, width, height, computeProgram);
    }

    private void initScreenQuad() {
        // Screen quad texture
        screenQuadTexture = new Texture(width, height, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture.active(0);
        screenQuadTexture.bind();
        screenQuadTexture.bindAsImage(0, GL_WRITE_ONLY, GL_RGBA32F);

        int texLocation = screenQuadProgram.getUniformLocation("tex_sampler");
        glUniform1i(texLocation, 0); // 0 corresponds to GL_TEXTURE0

        // Screen quad model
        screenQuad = new Quad(-1.0f, 1.0f, 2.0f, 2.0f);
    }

    private void initRaytraceExecutor() {
        raytraceExecutor = new RaytraceExecutor(screenQuadTexture, computeProgram);
        raytraceExecutor.addCompleteListener(
                () -> System.out.println("All samples have completed in " + raytraceExecutor.getFinishTime() + " ms.")
        );

        if (outputFile != null) {
            raytraceExecutor.addCompleteListener(() -> {
                System.out.println("Saving the result to " + outputFile + "...");
                screenQuadTexture.saveAsPNG(outputFile);
                System.out.println("Saved.");
            });
        }
    }

    /**
     * Draw the raytracing result to the frame buffer.
     */
    private void drawResult() {
        screenQuadProgram.use();
        Texture.active(0);
        screenQuadTexture.bind();
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

            if (!raytraceExecutor.sampleComplete())
                raytraceExecutor.raytrace();

            drawResult();
            guiRenderer.draw();

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            // Multi Viewports things.
            final long backupWindowPtr = glfwGetCurrentContext();
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

    public RaytraceExecutor getRaytraceExecutor() {
        return raytraceExecutor;
    }
}
