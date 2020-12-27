package posidon.uranium.graphics

import org.lwjgl.opengl.*
import posidon.uranium.events.Event
import posidon.uranium.events.WindowResizedEvent
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Eye
import java.nio.ByteBuffer

class Filter(fragmentPath: String, colorBufferCount: Int, val preRender: (shader: Shader, eye: Eye) -> Unit) : Node(), Renderer.FrameBuffer {

    private abstract class Buffer {
        abstract val texture: Texture?

        abstract fun init()
        abstract fun onWindowResize()

        fun destroy() {
            texture?.destroy()
        }
    }

    private class ColorBuffer(val colorAttachment: Int) : Buffer() {

        override var texture: Texture? = null

        override fun init() {
            texture = createTextureAttachment(Window.width, Window.height, colorAttachment)
        }

        override fun onWindowResize() {
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, Window.width, Window.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
        }

        private inline fun createTextureAttachment(width: Int, height: Int, colorAttachment: Int): Texture {
            val id = GL11.glGenTextures()
            val texture = Texture(id)
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            texture.setWrap(Texture.Wrap.CLAMP_TO_EDGE)
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, colorAttachment, id, 0)
            return texture
        }
    }

    private class DepthBuffer : Buffer() {

        override var texture: Texture? = null

        override fun init() {
            texture = createDepthTextureAttachment(Window.width, Window.height)
        }

        override fun onWindowResize() {
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, Window.width, Window.height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null as ByteBuffer?)
        }

        private inline fun createDepthTextureAttachment(width: Int, height: Int): Texture {
            val id = GL11.glGenTextures()
            val texture = Texture(id)
            Texture.bind(texture)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null as ByteBuffer?)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            texture.setWrap(Texture.Wrap.CLAMP_TO_EDGE)
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, id, 0)
            return texture
        }
    }

    var enabled: Boolean = true

    private val attachments = Array(colorBufferCount + 1) {
        if (it < colorBufferCount) {
            ColorBuffer(GL30.GL_COLOR_ATTACHMENT0 + it)
        } else DepthBuffer()
    }

    private var id = 0
    private val shader = Shader("/shaders/postVertex.glsl", fragmentPath)

    init {
        Renderer.runOnThread {
            id = createFrameBuffer()
            attachments.forEach { it.init() }
            GL20.glDrawBuffers(IntArray(colorBufferCount) {
                GL30.GL_COLOR_ATTACHMENT0 + it
            })
            shader.create()
        }
    }

    override fun bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id)
        GL11.glViewport(0, 0, Window.width, Window.height)
    }

    override fun clear() {
        GL11.glClearColor(0f, 0f, 0f, 1.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
    }

    override fun onEvent(event: Event) {
        if (event is WindowResizedEvent) {
            attachments.forEach { it.onWindowResize() }
        }
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
        val currentFilter = Scene.nextBuffer(this)

        shader.bind()
        preRender(shader, eye)

        Texture.bind(*attachments.map { it.texture }.toTypedArray())

        Renderer.render(Mesh.QUAD)
        bind()
        clear()
        currentFilter.bind()
    }
}