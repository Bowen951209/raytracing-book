package net.bowen.system;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram extends Deleteable{

    private final int programId;
    private final Set<Shader> attachedShaders = new HashSet<>();
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    public ShaderProgram() {
        super(true);
        programId = glCreateProgram();
        System.out.println("Shader program(" + programId + ") created.");
    }

    @Override
    public void delete() {
        glDeleteProgram(programId);
        System.out.println("Shader program(" + programId + ") deleted.");
    }

    public void attachShader(Shader shader) {
        glAttachShader(programId, shader.getId());
        attachedShaders.add(shader);

        System.out.println("Shader(" + shader.getId() + ") attached to program(" + programId + ").");
    }

    public void link() {
        glLinkProgram(programId);

        // Check for linking errors
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error linking shader program: " + glGetProgramInfoLog(programId));
        }

        System.out.println("Shader program(" + programId + ") linked.");

        // Detach and delete shaders after linking
        detachAndDeleteShaders();
    }

    public void use() {
        glUseProgram(programId);
    }

    public int getUniformLocation(String name) {
        Integer location;
        if ((location = uniformLocations.get(name)) == null) {
            use();
            location = glGetUniformLocation(programId, name);
            uniformLocations.put(name, location);
        }
        return location;
    }

    public void setUniform1iv(String name, int[] value) {
        use(); // Use the program first to put the uniform.
        glUniform1iv(getUniformLocation(name), value);
    }

    private void detachAndDeleteShaders() {
        for (Shader shader : attachedShaders) {
            glDetachShader(programId, shader.getId());
            System.out.println("Shader(" + shader.getId() + ") detached from program(" + programId + ").");
            shader.delete();
        }
    }
}
