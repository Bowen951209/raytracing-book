package net.bowen.draw;

import net.bowen.draw.materials.*;
import net.bowen.draw.models.raytrace.*;
import net.bowen.draw.textures.*;
import net.bowen.system.ShaderProgram;
import org.joml.Vector3f;

import java.util.Random;

public final class Scene {
    public final Camera camera = new Camera();
    private final ShaderProgram computeProgram;

    public Scene(int sceneID, int initImageWidth, int initImageHeight, ShaderProgram computeProgram) {
        this.computeProgram = computeProgram;
        RaytraceModel.initSSBOs();

        switch (sceneID) {
            case 0 -> bouncingSpheres();
            case 1 -> checkerSpheres();
            case 2 -> earth();
            case 3 -> perlinSpheres();
            case 4 -> quads();
            case 5 -> simpleLight();
            case 6 -> cornellBox();
            case 7 -> cornellSmoke();
            case 8 -> finalScene();
            default -> throw new IllegalArgumentException("Invalid scene ID: " + sceneID);
        }

        Texture.putTextureIndices(computeProgram);
        camera.setImageSize(initImageWidth, initImageHeight);
        camera.init(computeProgram);
    }

    public void updateCamera(int imageWidth, int imageHeight) {
        camera.setImageSize(imageWidth, imageHeight);
        camera.calculateProperties();
        camera.putToShaderProgram(computeProgram);
    }

    private void bouncingSpheres() {
        SolidTexture.init();
        CheckerTexture.init();

        Material mat = new Lambertian(SolidTexture.registerColor(0.5f, 0.5f, 0.5f));
        RaytraceModel.addModel(new Sphere(0, -1, 0, 0.5f, mat));
        Material groundMaterial = new Lambertian(CheckerTexture.registerColor(.2f, .3f, .1f, .9f, .9f, .9f, .32f));
        RaytraceModel.addModel(new Sphere(0, -1000, 0, 1000, groundMaterial));

        Random random = new Random();
        for (int a = -11; a < 11; a++) {
            for (int b = -11; b < 11; b++) {
                double chooseMaterial = Math.random();
                Vector3f center = new Vector3f(
                        (float) (a + 0.9f * Math.random()),
                        0.2f,
                        (float) (b + 0.9f * Math.random())
                );

                if ((new Vector3f(center).sub(4, 0.2f, 0)).length() > 0.9f) {
                    Material sphereMaterial;

                    if (chooseMaterial < 0.8) {
                        // diffuse
                        Color albedo = Color.randomColor().mul(Color.randomColor());
                        sphereMaterial = new Lambertian(SolidTexture.registerColor(albedo));
                        Vector3f center2 = new Vector3f(center).add(new Vector3f(0, (float) (Math.random() * 0.5f), 0));
                        RaytraceModel.addModel(new Sphere(center, center2, 0.2f, sphereMaterial));
                    } else if (chooseMaterial < 0.95) {
                        // metal
                        Color albedo = Color.randomColor(0.5f, 1);
                        float fuzz = random.nextFloat(0, 0.5f);
                        sphereMaterial = new Metal(SolidTexture.registerColor(albedo), fuzz);
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

        Material material2 = new Lambertian(SolidTexture.registerColor(0.4f, 0.2f, 0.1f));
        RaytraceModel.addModel(new Sphere(-4, 1, 0, 1, material2));

        Material material3 = new Metal(SolidTexture.registerColor(0.7f, 0.6f, 0.5f), 0);
        RaytraceModel.addModel(new Sphere(4, 1, 0, 1, material3));

        RaytraceModel.putModelsToProgram();
        SolidTexture.putDataToTexture();
        CheckerTexture.putDataToTexture();

        camera.setVerticalFOV(20);
        camera.setLookFrom(13, 2, 3);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0.6f);
        camera.setFocusDist(10);
        camera.setBackground(0.70f, 0.80f, 1.00f);
    }

    private void checkerSpheres() {
        CheckerTexture.init();

        int checker1 = CheckerTexture.registerColor(.2f, .3f, .1f, .9f, .9f, .9f, 0.32f);
        int checker2 = CheckerTexture.registerColor(.5f, .2f, .1f, .9f, .9f, .9f, 0.32f);

        RaytraceModel.addModel(new Sphere(0, -10, 0, 10, new Lambertian(checker1)));
        RaytraceModel.addModel(new Sphere(0, 10, 0, 10, new Lambertian(checker2)));

        RaytraceModel.putModelsToProgram();
        CheckerTexture.putDataToTexture();

        camera.setVerticalFOV(20);
        camera.setLookFrom(13, 2, 3);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0.70f, 0.80f, 1.00f);
    }

    private void earth() {
        Texture earthTexture = ImageTexture.create("textures/earthmap.jpg");
        Material earthSurface = new Lambertian(earthTexture);
        Sphere globe = new Sphere(0, 0, 0, 2, earthSurface);

        RaytraceModel.addModel(globe);
        RaytraceModel.putModelsToProgram();

        camera.setVerticalFOV(20);
        camera.setLookFrom(0, 0, 12);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0.70f, 0.80f, 1.00f);
    }

    private void perlinSpheres() {
        PerlinNoiseTexture perlinNoise = PerlinNoiseTexture.create(4);

        Material groundSurface = new Lambertian(perlinNoise);
        RaytraceModel.addModel(new Sphere(0, -1000, 0, 1000, groundSurface));

        Material sphereSurface = new Lambertian(perlinNoise);
        RaytraceModel.addModel(new Sphere(0, 2, 0, 2, sphereSurface));

        RaytraceModel.putModelsToProgram();

        camera.setVerticalFOV(20);
        camera.setLookFrom(13, 2, 3);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0.70f, 0.80f, 1.00f);
    }

    private void quads() {
        SolidTexture.init();

        Material leftRed = new Lambertian(SolidTexture.registerColor(1.0f, 0.2f, 0.2f));
        Material backGreen = new Lambertian(SolidTexture.registerColor(0.2f, 1.0f, 0.2f));
        Material rightBlue = new Lambertian(SolidTexture.registerColor(0.2f, 0.2f, 1.0f));
        Material upperOrange = new Lambertian(SolidTexture.registerColor(1.0f, 0.5f, 0.0f));
        Material lowerTeal = new Lambertian(SolidTexture.registerColor(0.2f, 0.8f, 0.8f));

        RaytraceModel.addModel(new Quad(new Vector3f(-3, -2, 5), new Vector3f(0, 0, -4), new Vector3f(0, 4, 0), leftRed));
        RaytraceModel.addModel(new Quad(new Vector3f(-2, -2, 0), new Vector3f(4, 0, 0), new Vector3f(0, 4, 0), backGreen));
        RaytraceModel.addModel(new Quad(new Vector3f(3, -2, 1), new Vector3f(0, 0, 4), new Vector3f(0, 4, 0), rightBlue));
        RaytraceModel.addModel(new Quad(new Vector3f(-2, 3, 1), new Vector3f(4, 0, 0), new Vector3f(0, 0, 4), upperOrange));
        RaytraceModel.addModel(new Quad(new Vector3f(-2, -3, 5), new Vector3f(4, 0, 0), new Vector3f(0, 0, -4), lowerTeal));

        RaytraceModel.putModelsToProgram();
        SolidTexture.putDataToTexture();

        camera.setVerticalFOV(80);
        camera.setLookFrom(0, 0, 9);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0.70f, 0.80f, 1.00f);
    }

    private void simpleLight() {
        PerlinNoiseTexture perlinNoise = PerlinNoiseTexture.create(4);

        RaytraceModel.addModel(new Sphere(0, -1000, 0, 1000, new Lambertian(perlinNoise)));
        RaytraceModel.addModel(new Sphere(0, 2, 0, 2, new Lambertian(perlinNoise)));

        Material diffLight = new DiffuseLight(new Color(4, 4, 4));
        RaytraceModel.addModel(new Sphere(
                new Vector3f(0, 7, 0),
                2,
                diffLight
        ));
        RaytraceModel.addModel(new Quad(
                new Vector3f(3, 1, -2),
                new Vector3f(2, 0, 0),
                new Vector3f(0, 2, 0),
                diffLight
        ));

        RaytraceModel.putModelsToProgram();

        camera.setVerticalFOV(20);
        camera.setLookFrom(26, 3, 6);
        camera.setLookAt(0, 2, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0, 0, 0);
    }

    private void cornellBox() {
        SolidTexture.init();

        Material red = new Lambertian(SolidTexture.registerColor(0.65f, 0.05f, 0.05f));
        Material white = new Lambertian(SolidTexture.registerColor(0.73f, 0.73f, 0.73f));
        Material green = new Lambertian(SolidTexture.registerColor(0.12f, 0.45f, 0.15f));
        Material light = new DiffuseLight(new Color(15, 15, 15));
        Material aluminum = new Metal(SolidTexture.registerColor(0.8f, 0.85f, 0.88f), 0.0f);

        Quad lightQuad = new Quad(new Vector3f(343, 554, 332), new Vector3f(-130, 0, 0), new Vector3f(0, 0, -105), light);

        RaytraceModel.addModel(new Quad(new Vector3f(555, 0, 0), new Vector3f(0, 555, 0), new Vector3f(0, 0, 555), green));
        RaytraceModel.addModel(new Quad(new Vector3f(0, 0, 0), new Vector3f(0, 555, 0), new Vector3f(0, 0, 555), red));
        RaytraceModel.addModel(lightQuad);
        RaytraceModel.setLight(lightQuad);
        RaytraceModel.addModel(new Quad(new Vector3f(0, 0, 0), new Vector3f(555, 0, 0), new Vector3f(0, 0, 555), white));
        RaytraceModel.addModel(new Quad(new Vector3f(555, 555, 555), new Vector3f(-555, 0, 0), new Vector3f(0, 0, -555), white));
        RaytraceModel.addModel(new Quad(new Vector3f(0, 0, 555), new Vector3f(555, 0, 0), new Vector3f(0, 555, 0), white));

        Vector3f box1Translation = new Vector3f(265,0,295);
        Vector3f box1Rotation = new Vector3f(0, (float) Math.toRadians(15), 0);
        Box box1 = new Box(new Vector3f(0, 0, 0), new Vector3f(165,330,165), box1Translation, box1Rotation, aluminum);

        Vector3f box2Translation = new Vector3f(130,0,65);
        Vector3f box2Rotation = new Vector3f(0, (float) Math.toRadians(-18), 0);
        Box box2 = new Box(new Vector3f(0, 0, 0), new Vector3f(165,165,165), box2Translation, box2Rotation, white);

        RaytraceModel.addModel(box1);
        RaytraceModel.addModel(box2);

        RaytraceModel.putModelsToProgram();
        SolidTexture.putDataToTexture();

        camera.setVerticalFOV(40);
        camera.setLookFrom(278, 278, -800);
        camera.setLookAt(278, 278, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0, 0, 0);
    }

    private void cornellSmoke() {
        SolidTexture.init();

        Material red = new Lambertian(SolidTexture.registerColor(0.65f, 0.05f, 0.05f));
        Material white = new Lambertian(SolidTexture.registerColor(0.73f, 0.73f, 0.73f));
        Material green = new Lambertian(SolidTexture.registerColor(0.12f, 0.45f, 0.15f));
        Material light = new DiffuseLight(new Color(7, 7, 7));

        RaytraceModel.addModel(new Quad(new Vector3f(555, 0, 0), new Vector3f(0, 555, 0), new Vector3f(0, 0, 555), green));
        RaytraceModel.addModel(new Quad(new Vector3f(0, 0, 0), new Vector3f(0, 555, 0), new Vector3f(0, 0, 555), red));
        RaytraceModel.addModel(new Quad(new Vector3f(113, 554, 127), new Vector3f(330, 0, 0), new Vector3f(0, 0, 305), light));
        RaytraceModel.addModel(new Quad(new Vector3f(0, 555, 0), new Vector3f(555, 0, 0), new Vector3f(0, 0, 555), white));
        RaytraceModel.addModel(new Quad(new Vector3f(0, 0, 0), new Vector3f(555, 0, 0), new Vector3f(0, 0, 555), white));
        RaytraceModel.addModel(new Quad(new Vector3f(0, 0, 555), new Vector3f(555, 0, 0), new Vector3f(0, 555, 0), white));

        Box box1 = new Box(new Vector3f(0, 0,0), new Vector3f(165,330,165), new Vector3f(265, 0, 295), new Vector3f(0, (float) Math.toRadians(15), 0), white);
        Box box2 = new Box(new Vector3f(0, 0,0), new Vector3f(165,165,165), new Vector3f(130, 0, 65), new Vector3f(0, (float) Math.toRadians(-18), 0), white);

        RaytraceModel.addModel(new ConstantMedium(box1, 0.01f, new Isotropic(SolidTexture.registerColor(0.0f, 0.0f, 0.0f))));
        RaytraceModel.addModel(new ConstantMedium(box2, 0.01f, new Isotropic(SolidTexture.registerColor(1, 1, 1))));

        RaytraceModel.putModelsToProgram();
        SolidTexture.putDataToTexture();

        camera.setVerticalFOV(40);
        camera.setLookFrom(278, 278, -800);
        camera.setLookAt(278, 278, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0, 0, 0);
    }

    private void finalScene() {
        SolidTexture.init();

        Material ground = new Lambertian(SolidTexture.registerColor(0.48f, 0.83f, 0.53f));

        int boxesPerSide = 20;
        for (int i = 0; i < boxesPerSide; i++) {
            for (int j = 0; j < boxesPerSide; j++) {
                float w = 100.0f;
                float x0 = -1000.0f + i*w;
                float z0 = -1000.0f + j*w;
                float y0 = 0.0f;
                float x1 = x0 + w;
                float y1 = (float) (1 + Math.random() * 100);
                float z1 = z0 + w;
                RaytraceModel.addModel(new Box(new Vector3f(x0, y0, z0), new Vector3f(x1, y1, z1), ground));
            }
        }

        Material light = new DiffuseLight(new Color(7, 7, 7));
        RaytraceModel.addModel(new Quad(new Vector3f(123,554,147), new Vector3f(300, 0, 0), new Vector3f(0, 0, 265), light));

        Vector3f center1 = new Vector3f(400, 400, 200);
        Vector3f center2 = new Vector3f(center1).add(new Vector3f(100, 0, 0));
        Material sphereMaterial = new Lambertian(SolidTexture.registerColor(0.7f, 0.3f, 0.1f));
        RaytraceModel.addModel(new Sphere(center1, center2, 50, sphereMaterial));

        RaytraceModel.addModel(new Sphere(new Vector3f(260, 150, 45), 50, new Dielectric(1.5f)));
        RaytraceModel.addModel(new Sphere(new Vector3f(0, 150, 145), 50, new Metal(SolidTexture.registerColor(0.8f, 0.8f, 0.9f), 0.999f)));


        Sphere boundary = new Sphere(new Vector3f(360, 150, 145), 70, new Dielectric(1.5f));
        RaytraceModel.addModel(boundary);
        RaytraceModel.addModel(new ConstantMedium(boundary, 0.2f, new Isotropic(SolidTexture.registerColor(0.2f, 0.4f, 0.9f))));

        boundary = new Sphere(new Vector3f(0, 0, 0), 5000, new Dielectric(1.5f));
        RaytraceModel.addModel(new ConstantMedium(boundary, 0.0001f, new Isotropic(SolidTexture.registerColor(1, 1, 1))));

        Material earthMaterial = new Lambertian(ImageTexture.create("textures/earthmap.jpg", 100, 0));
        RaytraceModel.addModel(new Sphere(new Vector3f(400, 200, 400), 100, earthMaterial));

        Texture perlinTexture = PerlinNoiseTexture.create(0.2f);
        Material noiseMaterial = new Lambertian(perlinTexture);
        RaytraceModel.addModel(new Sphere(new Vector3f(220, 280, 300), 80, noiseMaterial));

        Lambertian white = new Lambertian(SolidTexture.registerColor(0.73f, 0.73f, 0.73f));
        int ns = 1000;
        for (int j = 0; j < ns; j++) {
            Vector3f center = new Vector3f((float) (165 * Math.random()), (float) (165 * Math.random()), (float) (165 * Math.random()));
            center.add(-100, 270, 395);
            RaytraceModel.addModel(new Sphere(center, 10, white));
        }

        RaytraceModel.putModelsToProgram();
        SolidTexture.putDataToTexture();

        camera.setVerticalFOV(40);
        camera.setLookFrom( 478, 278, -600);
        camera.setLookAt(278, 278, 0);
        camera.setDefocusAngle(0);
        camera.setBackground(0, 0, 0);
    }
}
