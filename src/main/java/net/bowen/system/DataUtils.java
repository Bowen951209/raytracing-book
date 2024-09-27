package net.bowen.system;

import org.joml.Vector3f;

import java.nio.FloatBuffer;

public class DataUtils {
    public static void putToBuffer(Vector3f vec, FloatBuffer buffer) {
        buffer.put(vec.x);
        buffer.put(vec.y);
        buffer.put(vec.z);
    }
}
