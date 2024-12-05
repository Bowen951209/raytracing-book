package net.bowen.system;

import net.bowen.exceptions.InvalidShaderTypeException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

public class Shader extends Deleteable {
    private record ShaderSource(boolean isMainSource, String filename, String source) {
    }

    private static final Pattern INCLUDE_PATTERN = Pattern.compile("#include\\s+<(.+?)>");

    private final List<ShaderSource> sources = new ArrayList<>();
    private final int shaderId;

    // Constructor for vertex, fragment, or compute shaders
    public Shader(String resourcePath, int type) {
        super(false);
        String source = readSources(resourcePath);

        shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check for compilation errors
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String glLog = glGetShaderInfoLog(shaderId);
            String processedLog = splitFilesLog(glLog);
            throw new RuntimeException(
                    "Error creating shader. \n--- Log: ---\n" + processedLog + "\n--- GL Original Log: ---\n" + glLog
            );
        }

        System.out.println("Shader(" + shaderId + ")[" + getTypeName(type) + "] created.");
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

    /**
     * Convert the OpenGL compile log to the string that indicates what file and which line an error occurs.
     */
    private String splitFilesLog(String glLog) {
        // Parse log lines with format `0(<line>) : `
        Pattern logPattern = Pattern.compile("0\\((\\d+)\\) : ");
        Matcher logMatcher = logPattern.matcher(glLog);

        StringBuilder result = new StringBuilder();

        int lastMatchEnd = 0;
        while (logMatcher.find()) {
            int globalLine = Integer.parseInt(logMatcher.group(1));
            int localLine = 0;
            String sourceFile = "";

            int lineCount = 0;
            int numLinesBefore = 0;
            int numMainSourceLinesBefore = 0;
            for (ShaderSource shaderSource : sources) {
                int numLinesThisSource = (int) shaderSource.source.chars().filter(c -> c == '\n').count();
                lineCount += numLinesThisSource;

                if (lineCount >= globalLine) {
                    sourceFile = shaderSource.filename;
                    localLine = shaderSource.isMainSource ?
                            globalLine - numLinesBefore + numMainSourceLinesBefore :
                            globalLine - numLinesBefore;
                    break;
                }

                numLinesBefore = lineCount;
                if (shaderSource.isMainSource) numMainSourceLinesBefore += numLinesThisSource;
            }

            result.append(glLog, lastMatchEnd, logMatcher.start());
            result.append("<").append(sourceFile).append(">(").append(localLine).append(") : ");

            lastMatchEnd = logMatcher.end();
        }
        result.append(glLog, lastMatchEnd, glLog.length());

        return result.toString();
    }

    private String readSources(String resourcePath) {
        // Read the raw source:
        String rawSource = readRaw(resourcePath);

        // Process source:
        Matcher includeMatcher = INCLUDE_PATTERN.matcher(rawSource);

        int lastMatchEnd = 0;
        // Iterate over all matches of `#include` directives
        while (includeMatcher.find()) {
            // Add the part of main source
            sources.add(new ShaderSource(true, resourcePath, rawSource.substring(lastMatchEnd, includeMatcher.start())));

            // Add include source
            String includePath = includeMatcher.group(1);
            String includeContent = readRaw("shaders/" + includePath);
            sources.add(new ShaderSource(false, includePath, includeContent));

            // Update the end position of the last match
            lastMatchEnd = includeMatcher.end();
        }

        // Add the remaining content of the main source
        sources.add(new ShaderSource(true, resourcePath, rawSource.substring(lastMatchEnd)));

        StringBuilder finalSource = new StringBuilder();
        for (ShaderSource shaderSource : sources)
            finalSource.append(shaderSource.source);

        return finalSource.toString();
    }

    private static String readRaw(String resourcePath) {
        try (InputStream inputStream = Shader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
