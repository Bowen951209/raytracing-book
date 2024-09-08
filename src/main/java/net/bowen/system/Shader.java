package net.bowen.system;

import net.bowen.exceptions.InvalidShaderTypeException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

public class Shader extends Deleteable {

    private final int shaderId;

    // Constructor for vertex, fragment, or compute shaders
    public Shader(String resourcePath, int type) {
        super(false);
        // Read the shader source from the file
        String source;
        try {
            Path path = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(resourcePath)).toURI());
            source = new String(Files.readAllBytes(path));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check for compilation errors
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error creating shader: " + glGetShaderInfoLog(shaderId));
        }

        System.out.println("Shader(" + shaderId + ")[" + getTypeName(type)+ "] created.");
    }

    @Override
    public void delete() {
        glDeleteShader(shaderId);
        System.out.println("Shader(" + shaderId + ") deleted.");
    }

    public int getId() {
        return shaderId;
    }

    private static String getTypeName(int type) {
        switch (type) {
            case GL_VERTEX_SHADER -> {
                return "vertex";
            }
            case GL_FRAGMENT_SHADER -> {
                return "fragment";
            }
            case GL_COMPUTE_SHADER -> {
                return "compute";
            }
            default -> throw new InvalidShaderTypeException("Invalid type int of: " + type);
        }
    }
}
