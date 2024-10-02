package net.bowen.gui;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.bowen.draw.Color;
import net.bowen.draw.*;
import net.bowen.draw.material.Dielectric;
import net.bowen.draw.material.Lambertian;
import net.bowen.draw.material.Material;
import net.bowen.draw.material.Metal;
import net.bowen.system.*;
import org.joml.Vector3f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private final String title;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private long windowHandle;
    private int width, height;
    private Quad screenQuad;
    private Texture quadTexture;
    private GuiRenderer guiRenderer;
    private RaytraceExecutor raytraceExecutor;
    private Camera camera;

    ShaderProgram quadProgram, computeProgram;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;

        System.out.println("LWJGL version: " + Version.getVersion());

        init();
        loop();
        free();
    }

    private void init() {
        System.out.println("Initializing...");
        long startTime = System.currentTimeMillis();
        initGLFW();
        initShaderPrograms();
        initTextures();
        initModels();
        initRaytraceExecutor();
        initImGui();
        float initTime = (System.currentTimeMillis() - startTime) / 1000f;
        System.out.println("Initialization completed in " + initTime + " sec.");

        // Upload the max depth uniform
        guiRenderer.maxBounceSliderSlide();

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
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height)->{
            this.width = width;
            this.height = height;

            // Resize GL viewport.
            glViewport(0, 0, width, height);

            // Resize textures and camera.
            quadTexture.resize(width, height);
            camera.setImageSize(width, height);
            camera.calculateProperties();
            camera.putToShaderProgram();

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
        quadProgram = new ShaderProgram();
        quadProgram.attachShader(new Shader("shaders/plainTextureShaders/vert.glsl", GL_VERTEX_SHADER));
        quadProgram.attachShader(new Shader("shaders/plainTextureShaders/frag.glsl", GL_FRAGMENT_SHADER));
        quadProgram.link();

        computeProgram = new ShaderProgram();
        computeProgram.attachShader(new Shader("shaders/raytrace/compute.glsl", GL_COMPUTE_SHADER));
        computeProgram.link();
    }

    private void initModels() {
        // Drawable models:
        screenQuad = new Quad(-1.0f, 1.0f, 2.0f, 2.0f);

        RaytraceModel.initSSBO();
        Material mat = new Lambertian(0.5f, 0.5f, 0.5f);
        RaytraceModel.addModel(new Sphere(0, -1, 0, 0.5f, mat));
        Material groundMaterial = new Lambertian(0.5f, 0.5f, 0.5f);
        RaytraceModel.addModel(new Sphere(0, -1000, 0, 1000, groundMaterial));

        Random random = new Random();
        Vector3f center = new Vector3f();
        for (int a = -11; a < 11; a++) {
            for (int b = -11; b < 11; b++) {
                double chooseMaterial = Math.random();
                center.set(a + 0.9f * Math.random(), 0.2f, b + 0.9f * Math.random());

                if ((new Vector3f(center).sub(4, 0.2f, 0)).length() > 0.9f) {
                    Material sphereMaterial;

                    if (chooseMaterial < 0.8) {
                        // diffuse
                        Color albedo = Color.randomColor().mul(Color.randomColor());
                        sphereMaterial = new Lambertian(albedo);
                        RaytraceModel.addModel(new Sphere(center, 0.2f, sphereMaterial));
                    } else if (chooseMaterial < 0.95) {
                        // metal
                        Color albedo = Color.randomColor(0.5f, 1);
                        float fuzz = random.nextFloat(0, 0.5f);
                        sphereMaterial = new Metal(albedo, fuzz);
                        RaytraceModel.addModel(new Sphere(center, 0.2f, sphereMaterial));
                    } else {
                        // glass
                        sphereMaterial = new Dielectric(1.5f);
                        RaytraceModel.addModel(new Sphere(center, 0.2f, sphereMaterial));
                    }
                }
            }
        }

        Material material1 = new Dielectric(1.5f);
        RaytraceModel.addModel(new Sphere(0, 1, 0, 1, material1));

        Material material2 = new Lambertian(0.4f, 0.2f, 0.1f);
        RaytraceModel.addModel(new Sphere(-4, 1, 0, 1, material2));

        Material material3 = new Metal(0.7f, 0.6f, 0.5f, 0);
        RaytraceModel.addModel(new Sphere(4, 1, 0, 1, material3));

        RaytraceModel.putModelsToProgram();

        camera = new Camera();

        camera.setImageSize(width, height);
        camera.setVerticalFOV(20);
        camera.setLookFrom(13, 2, 3);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0.6f);
        camera.setFocusDist(10);
        camera.init();
    }

    private void initTextures() {
        quadTexture = new Texture(width, height, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        quadTexture.bind();
        quadTexture.bindAsImage(0, GL_WRITE_ONLY, GL_RGBA32F);
        Texture.active(0);

        int texLocation = quadProgram.getUniformLocation("tex_sampler");
        glUniform1i(texLocation, 0); // 0 corresponds to GL_TEXTURE0
    }

    private void initRaytraceExecutor() {
        raytraceExecutor = new RaytraceExecutor(quadTexture, computeProgram);
        raytraceExecutor.addCompleteListener(
                () -> System.out.println("All samples have completed in " + raytraceExecutor.getFinishTime() + " ms.")
        );
    }

    /**
     * Draw the raytracing result to the frame buffer.
     */
    private void drawResult() {
        quadProgram.use();
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
