package net.bowen.system;

import net.bowen.draw.BVHNode;
import net.bowen.draw.Color;
import net.bowen.draw.RaytraceModel;
import net.bowen.draw.Sphere;
import net.bowen.draw.material.Dielectric;
import net.bowen.draw.material.Lambertian;
import net.bowen.draw.material.Material;
import net.bowen.draw.material.Metal;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BVHNodeTest {
    @Test
    void NodesContainAllSpheres() {
        // The classic scene.
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

        // Spheres are added to RaytraceModel.SPHERES.

        // Create BVH nodes.
        new BVHNode(RaytraceModel.SPHERES, 0, RaytraceModel.SPHERES.size());
        // Each BVHNode will put itself to RaytraceModel.BVH_NODES when they are created.
        var nodes = RaytraceModel.BVH_NODES;

        // Make sure all spheres are contained in all nodes.
        Set<Float> sphereIds = new HashSet<>();
        for (BVHNode node : nodes) {
            float leftIdx = node.left.id;
            float rightIdx = node.right.id;

            // If left is sphere:
            if (isSphere(leftIdx))
                sphereIds.add(leftIdx);

            // If right is sphere:
            if (isSphere(rightIdx))
                sphereIds.add(rightIdx);
        }

        Set<Float> spheresInRaytraceModel = new HashSet<>(RaytraceModel.SPHERES.size());
        for (Sphere sphere : RaytraceModel.SPHERES) {
            spheresInRaytraceModel.add(sphere.id);
        }

        assertEquals(spheresInRaytraceModel, sphereIds);
    }

    private static boolean isSphere(float id) {
        float modelId = id - (int) id;
        return  0.01f < modelId && modelId <= (Sphere.MODEL_ID + 0.01f);
    }
}