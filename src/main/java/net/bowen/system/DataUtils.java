package net.bowen.system;

import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class DataUtils {
    public static void putToBuffer(Vector3f vec, FloatBuffer buffer) {
        buffer.put(vec.x);
        buffer.put(vec.y);
        buffer.put(vec.z);
    }

    public static void putToBuffer(Vector3f vec, ByteBuffer buffer) {
        buffer.putFloat(vec.x);
        buffer.putFloat(vec.y);
        buffer.putFloat(vec.z);
    }
}
