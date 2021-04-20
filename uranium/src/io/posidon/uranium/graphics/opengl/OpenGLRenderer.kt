package io.posidon.uranium.graphics.opengl

import io.posidon.uranium.graphics.Mesh
import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.graphics.Texture
import io.posidon.uranium.graphics.Window
import io.posidon.uranium.nodes.ui.View
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.*
import org.lwjgl.stb.STBImage
import java.nio.ByteBuffer
import kotlin.math.min

class OpenGLRenderer(window: Window) : Renderer(window) {

    override fun render(mesh: Mesh) {
        mesh.bind()
        GL11C.glDrawElements(GL11.GL_TRIANGLES, mesh.vertexCount, GL11.GL_UNSIGNED_INT, 0)
    }

    fun renderLines(mesh: Mesh) {
        mesh.bind()
        GL11C.glDrawElements(GL11.GL_LINES, mesh.vertexCount, GL11.GL_UNSIGNED_INT, 0)
    }

    override fun init(windowId: Long) {
        GL11C.glEnable(GL11.GL_CULL_FACE)
        GL11C.glCullFace(GL11.GL_BACK)
        GL11C.glEnable(GL11.GL_BLEND)
        GL11C.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL13C.glActiveTexture(GL13.GL_TEXTURE0)

        Mesh.init()
        View.init()
    }

    override fun destroy() {
        GL20.glUseProgram(0)

        View.destroy()
        Mesh.destroy()
    }

    override fun preWindowInit() {
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE)

        // Antialiasing
        GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 4)
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4)
    }

    override fun colorBuffer(attachment: Int, width: Int, height: Int): Buffer =
        ColorBuffer(GL30.GL_COLOR_ATTACHMENT0 + attachment, width, height)

    override fun depthBuffer(width: Int, height: Int): Buffer =
        DepthBuffer(width, height)

    override fun enableDepthTest() {
        GL11C.glEnable(GL11C.GL_DEPTH_TEST)
    }

    override fun disableDepthTest() {
        GL11C.glDisable(GL11C.GL_DEPTH_TEST)
    }

    companion object {
        inline fun createTexture(buffer: ByteBuffer, width: Int, height: Int): Int {
            val id = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
            //GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, -1f)
            //GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE)
            if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                val amount = min(4f, GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT))
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount)
            } else {
                println("error: Anisotropic filtering isn't supported")
            }
            STBImage.stbi_image_free(buffer)
            return id
        }
    }

    class ColorBuffer(val colorAttachment: Int, width: Int, height: Int) : Buffer(width, height) {

        override var texture: Texture? = null

        override fun init() {
            texture = createTextureAttachment(width, height, colorAttachment)
        }

        override fun updateDimensions() {
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
        }

        private inline fun createTextureAttachment(width: Int, height: Int, colorAttachment: Int): Texture {
            val id = GL11.glGenTextures()
            val texture = Texture(id, width, height)
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            texture.setWrap(Texture.Wrap.CLAMP_TO_EDGE)
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, colorAttachment, id, 0)
            return texture
        }
    }

    class DepthBuffer(width: Int, height: Int) : Buffer(width, height) {

        override var texture: Texture? = null

        override fun init() {
            texture = createDepthTextureAttachment(width, height)
        }

        override fun updateDimensions() {
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null as ByteBuffer?)
        }

        private inline fun createDepthTextureAttachment(width: Int, height: Int): Texture {
            val id = GL11.glGenTextures()
            val texture = Texture(id, width, height)
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null as ByteBuffer?)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            texture.setWrap(Texture.Wrap.CLAMP_TO_EDGE)
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, id, 0)
            return texture
        }
    }
}