package io.posidon.uranium.graphics

import io.posidon.library.types.Mat4f
import io.posidon.library.types.Vec2f
import io.posidon.library.types.Vec3f
import io.posidon.library.types.Vec3i
import io.posidon.library.util.Resources
import io.posidon.uranium.util.Stack
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20C
import kotlin.system.exitProcess

class Shader(val vertexPath: String, val fragmentPath: String) {
    private var vertexID = 0
    private var fragmentID = 0
    var programID = 0
        private set

    fun create() {
        val libFile = "#version 420 core\n" + Resources.loadAsString("/shaders/lib.glsl") + "\n#line 1\n"
        val vertexFile = libFile + Resources.loadAsString(vertexPath)
        val fragmentFile = libFile + Resources.loadAsString(fragmentPath)

        programID = GL20C.glCreateProgram()
        vertexID = GL20C.glCreateShader(GL20C.GL_VERTEX_SHADER)
        GL20C.glShaderSource(vertexID, vertexFile)
        GL20C.glCompileShader(vertexID)
        if (GL20C.glGetShaderi(vertexID, GL20C.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("[SHADER ERROR | $vertexPath]: " + GL20C.glGetShaderInfoLog(vertexID))
            exitProcess(1)
        }
        fragmentID = GL20C.glCreateShader(GL20C.GL_FRAGMENT_SHADER)
        GL20C.glShaderSource(fragmentID, fragmentFile)
        GL20C.glCompileShader(fragmentID)
        if (GL20C.glGetShaderi(fragmentID, GL20C.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("[SHADER ERROR | $fragmentPath]: " + GL20C.glGetShaderInfoLog(fragmentID))
            exitProcess(1)
        }
        GL20C.glAttachShader(programID, vertexID)
        GL20C.glAttachShader(programID, fragmentID)
        GL20C.glLinkProgram(programID)
        if (GL20C.glGetProgrami(programID, GL20C.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("[SHADER ERROR - Linking]: " + GL20C.glGetProgramInfoLog(programID))
            exitProcess(1)
        }
        GL20C.glValidateProgram(programID)
        if (GL20C.glGetProgrami(programID, GL20C.GL_VALIDATE_STATUS) == GL11.GL_FALSE)
            System.err.println("[SHADER ERROR - Validation]: " + GL20C.glGetProgramInfoLog(programID))
    }

    inline fun uniLoc(name: String) = GL20C.glGetUniformLocation(programID, name)

    inline operator fun set(name: String, value: Float) = GL20C.glUniform1f(uniLoc(name), value)
    inline operator fun set(name: String, value: Int) = GL20C.glUniform1i(uniLoc(name), value)
    inline operator fun set(name: String, value: Boolean) = GL20C.glUniform1i(uniLoc(name), if (value) 1 else 0)
    inline operator fun set(name: String, value: Vec2f) = GL20C.glUniform2f(uniLoc(name), value.x, value.y)
    inline operator fun set(name: String, value: Vec3f) = GL20C.glUniform3f(uniLoc(name), value.x, value.y, value.z)
    inline operator fun set(name: String, value: Vec3i) = GL20C.glUniform3i(uniLoc(name), value.x, value.y, value.z)

    inline operator fun set(name: String, value: FloatArray) {
        for (i in value.indices) set("$name[$i]", value[i])
    }
    inline operator fun set(name: String, value: IntArray) {
        for (i in value.indices) set("$name[$i]", value[i])
    }
    inline operator fun set(name: String, value: BooleanArray) {
        for (i in value.indices) set("$name[$i]", value[i])
    }
    inline operator fun set(name: String, value: Array<Vec2f>) {
        for (i in value.indices) set("$name[$i]", value[i])
    }
    inline operator fun set(name: String, value: Array<Vec3f>) {
        for (i in value.indices) set("$name[$i]", value[i])
    }

    inline operator fun set(name: String, value: Mat4f) {
        Stack.push { stack ->
            val matrix = stack.mallocFloat(Mat4f.SIZE * Mat4f.SIZE)
            matrix.put(value.all).flip()
            GL20C.glUniformMatrix4fv(uniLoc(name), true, matrix)
        }
    }

    inline fun bind() = GL20C.glUseProgram(programID)

    fun destroy() {
        GL20C.glDetachShader(programID, vertexID)
        GL20C.glDetachShader(programID, fragmentID)
        GL20C.glDeleteShader(vertexID)
        GL20C.glDeleteShader(fragmentID)
        GL20C.glDeleteProgram(programID)
    }
}