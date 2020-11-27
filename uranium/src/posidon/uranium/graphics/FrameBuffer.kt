package posidon.uranium.graphics

import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL20
import posidon.library.types.Vec3f
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Eye
import posidon.uranium.voxel.VoxelChunkMap
import java.nio.ByteBuffer

object FrameBuffer : Node("framebuffer") {

    private abstract class Buffer {
        abstract val id: Int

        abstract fun init()
        abstract fun onWindowResize()

        fun destroy() {
            GL11.glDeleteTextures(id)
        }
    }

    private class ColorBuffer(val colorAttachment: Int) : Buffer() {
        override var id = 0
        override fun init() {
            id = createTextureAttachment(Window.width, Window.height, colorAttachment)
        }

        override fun onWindowResize() {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, Window.width, Window.height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
        }

        private inline fun createTextureAttachment(width: Int, height: Int, colorAttachment: Int): Int {
            val texture = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, colorAttachment, texture, 0)
            return texture
        }
    }

    private class DepthBuffer : Buffer() {
        override var id = 0
        override fun init() {
            id = createDepthTextureAttachment(Window.width, Window.height)
        }

        override fun onWindowResize() {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, Window.width, Window.height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null as ByteBuffer?)
        }

        private inline fun createDepthTextureAttachment(width: Int, height: Int): Int {
            val texture = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null as ByteBuffer?)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, texture, 0)
            return texture
        }
    }

    private val attachments = arrayOf(
        ColorBuffer(GL30.GL_COLOR_ATTACHMENT0),
        ColorBuffer(GL30.GL_COLOR_ATTACHMENT1),
        ColorBuffer(GL30.GL_COLOR_ATTACHMENT2),
        ColorBuffer(GL30.GL_COLOR_ATTACHMENT3),
        DepthBuffer())

    private var id = 0
    private val shader = Shader("/shaders/postVertex.glsl", "/shaders/postFragment.glsl")

    fun init() {
        id = createFrameBuffer()
        attachments.forEach { it.init() }
        GL20.glDrawBuffers(intArrayOf(
            GL30.GL_COLOR_ATTACHMENT0,
            GL30.GL_COLOR_ATTACHMENT1,
            GL30.GL_COLOR_ATTACHMENT2,
            GL30.GL_COLOR_ATTACHMENT3))
        shader.create()
    }

    fun bind(width: Int, height: Int) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id)
        GL11.glViewport(0, 0, width, height)
    }

    fun clear() {
        GL11.glClearColor(0f, 0f, 0f, 1.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
    }

    fun onWindowResize() {
        attachments.forEach { it.onWindowResize() }
    }

    private fun createFrameBuffer(): Int {
        val buffer = GL30.glGenFramebuffers()
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, buffer)
        return buffer
    }

    override fun destroy() {
        GL30.glDeleteFramebuffers(id)
        attachments.forEach { it.destroy() }
    }

    override fun render(renderer: Renderer, eye: Eye) {
        Window.bindBuffer()

        shader.bind()
        shader["ambientLight"] = Scene.environment.ambientLight
        shader["skyColor"] = Scene.environment.skyColor
        shader["skyLight"] = Scene.environment.skyLight
        shader["sunNormal"] = Scene.environment.sun?.normal ?: Vec3f.ZERO
        shader["view"] = eye.viewMatrix
        shader["rotation"] = eye.rotationMatrix
        shader["projection"] = Renderer.projectionMatrix

        for (i in attachments.indices) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, attachments[i].id)
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0)

        Renderer.render(Mesh.QUAD)
    }
}