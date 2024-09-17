package net.bowen.system;

import net.bowen.exceptions.InvalidShaderTypeException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

public class Shader extends Deleteable {
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("#include\\s+<(.+?)>");

    private final int shaderId;

    // Constructor for vertex, fragment, or compute shaders
    public Shader(String resourcePath, int type) {
        super(false);
        // Read the shader source from the file
        String source = getSource(resourcePath);

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

    private static String getSource(String resourcePath) {
        // Read the raw source:
        String rawSource = readRaw(resourcePath);

        // Process source:
        StringBuilder processedSource = new StringBuilder();
        Matcher matcher = INCLUDE_PATTERN.matcher(rawSource);

        int lastMatchEnd = 0;
        // Iterate over all matches of `#include` directives
        while (matcher.find()) {
            // Append the code before the current `#include` directive to the result
            processedSource.append(rawSource, lastMatchEnd, matcher.start());

            // Extract the path of the included file from the `#include` directive
            String includePath = matcher.group(1);

            // Read the included file
            String includeContent = readRaw("shaders/" + includePath);

            // Append the content of the included file to the result
            processedSource.append(includeContent);

            // Update the end position of the last match
            lastMatchEnd = matcher.end();
        }

        // Append the remaining content of the file after the last `#include` directive
        processedSource.append(rawSource.substring(lastMatchEnd));

        return processedSource.toString();
    }

    private static String readRaw(String resourcePath) {
        try {
            Path path = Paths.get(Objects.requireNonNull(Shader.class.getClassLoader().getResource(resourcePath)).toURI());
            return new String(Files.readAllBytes(path));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
