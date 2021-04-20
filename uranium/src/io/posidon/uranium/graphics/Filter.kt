package io.posidon.uranium.graphics

import io.posidon.uranium.events.Event
import io.posidon.uranium.events.WindowResizedEvent
import io.posidon.uranium.nodes.Node
import io.posidon.uranium.nodes.Scene
import io.posidon.uranium.nodes.spatial.Eye
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

open class Filter(
    fragmentPath: String,
    colorBufferCount: Int,
    renderer: Renderer,
    val preRender: (renderer: Renderer, shader: Shader, eye: Eye) -> Unit
) : Node(), Renderer.FrameBuffer {

    var enabled: Boolean = true

    private val attachments = Array(colorBufferCount + 1) {
        if (it < colorBufferCount) {
            renderer.colorBuffer(it)
        } else renderer.depthBuffer()
    }

    private var id = 0
    private val shader = Shader("/shaders/postVertex.glsl", fragmentPath)

    init {
        create(colorBufferCount)
    }

    private fun create(colorBufferCount: Int) {
        id = createFrameBuffer()
        attachments.forEach { it.init() }
        GL20.glDrawBuffers(IntArray(colorBufferCount) {
            GL30.GL_COLOR_ATTACHMENT0 + it
        })
        shader.create()
    }

    override fun bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id)
        GL11.glViewport(0, 0, attachments[0].width, attachments[0].height)
    }

    override fun clear() {
        GL11.glClearColor(0f, 0f, 0f, 1.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
    }

    override fun onEvent(event: Event) {
        if (event is WindowResizedEvent) {
            attachments.forEach { it.resizeToWindow(event.window) }
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
        val currentFilter = Scene.nextBuffer(this, renderer)

        shader.bind()
        preRender(renderer, shader, eye)

        Texture.bind(*attachments.map { it.texture }.toTypedArray())

        renderer.render(Mesh.QUAD)
        bind()
        clear()
        currentFilter.bind()
    }
}