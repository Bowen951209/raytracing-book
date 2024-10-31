package net.bowen.draw;

import net.bowen.draw.materials.Dielectric;
import net.bowen.draw.materials.Lambertian;
import net.bowen.draw.materials.Material;
import net.bowen.draw.materials.Metal;
import net.bowen.draw.textures.CheckerTexture;
import net.bowen.draw.textures.ImageTexture;
import net.bowen.draw.textures.Texture;
import net.bowen.draw.textures.PerlinNoiseTexture;
import net.bowen.system.ShaderProgram;
import org.joml.Vector3f;

import java.util.Random;

public final class Scene {
    public final Camera camera = new Camera();

    public Scene(int sceneID, int initImageWidth, int initImageHeight, ShaderProgram computeProgram) {
        RaytraceModel.initSSBOs();

        switch (sceneID) {
            case 0 -> bouncingSpheres();
            case 1 -> checkerSpheres();
            case 2 -> earth();
            case 3 -> perlinSpheres();
        }

        Texture.putTextureIndices(computeProgram);
        camera.setImageSize(initImageWidth, initImageHeight);
        camera.init();
    }

    public void updateCamera(int imageWidth, int imageHeight) {
        camera.setImageSize(imageWidth, imageHeight);
        camera.calculateProperties();
        camera.putToShaderProgram();
    }

    private void bouncingSpheres() {
        Material mat = new Lambertian(0.5f, 0.5f, 0.5f);
        RaytraceModel.addModel(new Sphere(0, -1, 0, 0.5f, mat));
        Material groundMaterial = new Lambertian(CheckerTexture.create(.2f, .3f, .1f, .9f, .9f, .9f, .32f));
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
                        Vector3f center2 = new Vector3f(center).add(new Vector3f(0, (float) (Math.random() * 0.5f), 0));
                        RaytraceModel.addModel(new Sphere(center, center2, 0.2f, sphereMaterial));
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

        camera.setVerticalFOV(20);
        camera.setLookFrom(13, 2, 3);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0.6f);
        camera.setFocusDist(10);
    }

    private void checkerSpheres() {
        Texture checker1 = CheckerTexture.create(.2f, .3f, .1f, .9f, .9f, .9f, 0.32f);
        Texture checker2 = CheckerTexture.create(.5f, .2f, .1f, .9f, .9f, .9f, 0.32f);

        RaytraceModel.addModel(new Sphere(0, -10, 0, 10, new Lambertian(checker1)));
        RaytraceModel.addModel(new Sphere(0, 10, 0, 10, new Lambertian(checker2)));

        RaytraceModel.putModelsToProgram();

        camera.setVerticalFOV(20);
        camera.setLookFrom(13, 2, 3);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0);
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
    }

    private void perlinSpheres() {
        PerlinNoiseTexture perlinNoise = PerlinNoiseTexture.create(7);

        Material groundSurface = new Lambertian(perlinNoise);
        RaytraceModel.addModel(new Sphere(0, -1000, 0, 1000, groundSurface));

        Material sphereSurface = new Lambertian(perlinNoise);
        RaytraceModel.addModel(new Sphere(0, 2, 0, 2, sphereSurface));

        RaytraceModel.putModelsToProgram();

        camera.setVerticalFOV(20);
        camera.setLookFrom(13, 2, 3);
        camera.setLookAt(0, 0, 0);
        camera.setDefocusAngle(0);
    }
}
