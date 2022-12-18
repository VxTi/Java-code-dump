package project.util;

import static org.lwjgl.opengl.GL20.*;

public class Shader {

    int shaderProgramId, fragmentShaderId, vertexShaderId;
    Resource fragmentShader;
    Resource vertexShader;

    /**
     * Shader Program loader. Usage: new Shader("directory Location", "file name") -> new Shader("C:/.../dirName", "shaderFile") 
     * Make sure the shader file ends with "_fs.glsl" or "_vs.glsl" (or change it in the code below)
     * @param directory
     * @param fileName
     */
    public Shader(String directory, String fileName) {
        this.fragmentShader = new Resource(directory + "/" + fileName + "_fs.glsl");
        this.vertexShader = new Resource(directory + "/" + fileName + "_vs.glsl");
        this.shaderProgramId = glCreateProgram();

        this.fragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);
        this.vertexShaderId   = glCreateShader(GL_VERTEX_SHADER);

        glShaderSource(fragmentShaderId, this.fragmentShader.getContent());
        glShaderSource(vertexShaderId, this.vertexShader.getContent());
        glCompileShader(fragmentShaderId);
        glCompileShader(vertexShaderId);

        if (glGetShaderi(fragmentShaderId, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(fragmentShaderId, 1024));
        }

        if (glGetShaderi(vertexShaderId, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(vertexShaderId, 1024));
        }

        glAttachShader(this.shaderProgramId, vertexShaderId);
        glAttachShader(this.shaderProgramId, fragmentShaderId);

        glBindAttribLocation(this.shaderProgramId, 0, "vertices");

        glLinkProgram(this.shaderProgramId);

        if (glGetProgrami(this.shaderProgramId, GL_LINK_STATUS) != 1)
        {
            System.err.println(glGetProgramInfoLog(this.shaderProgramId, 1024));
        }

        glValidateProgram(this.shaderProgramId);

        if (glGetProgrami(this.shaderProgramId, GL_VALIDATE_STATUS) != 1)
        {
            System.err.println(glGetProgramInfoLog(this.shaderProgramId, 1024));
        }
    }

    public void attachProgram() {
        glUseProgram(this.shaderProgramId);
    }

    public void detachProgram() {
        glUseProgram(0);
    }

    public void delete() {
        glDeleteShader(shaderProgramId);
    }

    public void uniform1f(String variable, float value) {
        int loc = glGetUniformLocation(this.shaderProgramId, variable);
        if (loc != -1)
        {
            glUniform1f(loc, value);
        }
    }

    public void uniform3f(String variable, float x, float y, float z) {
        int loc = glGetUniformLocation(this.shaderProgramId, variable);
        if (loc != -1) {
            glUniform3f(loc, x, y, z);
        }
    }

}

