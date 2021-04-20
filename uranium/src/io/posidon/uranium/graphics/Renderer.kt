package io.posidon.uranium.graphics

import io.posidon.uranium.graphics.opengl.OpenGLRenderer
import io.posidon.uranium.nodes.spatial.Eye
import java.util.concurrent.ConcurrentLinkedQueue

abstract class Renderer(val window: Window) {

    /**
     * Renders [mesh]
     */
    abstract fun render (mesh: Mesh)

    abstract fun init (windowId: Long)
    abstract fun destroy ()
    abstract fun preWindowInit()

    /**
     * Adds [fn] to the queue to run on the render thread
     */
    fun runOnThread(fn: () -> Unit) {
        eventQueue.add(fn)
    }

    fun executeEventsFromQueue() {
        val it = eventQueue.iterator()
        while (it.hasNext()) {
            it.next()()
            it.remove()
        }
    }

    private val eventQueue = ConcurrentLinkedQueue<() -> Unit>()

    /**
     * The eye from whose point of view the renderer is gonna render 3d objects
     */
    var eye: Eye? = null
        set(value) {
            field = value
            eye?.let { it.projectionMatrix.setFovAndAspectRatio(it.fov, window.width.toFloat() / window.height.toFloat()) }
        }

    abstract fun colorBuffer(
        attachment: Int,
        width: Int = window.width,
        height: Int = window.height
    ): Buffer

    abstract fun depthBuffer(
        width: Int = window.width,
        height: Int = window.height
    ): Buffer

    abstract fun enableDepthTest()
    abstract fun disableDepthTest()

    companion object {
        const val NEAR = 0.2f
        const val FAR = 600f

        fun get(window: Window): Renderer = OpenGLRenderer(window)
    }

    internal interface FrameBuffer {
        fun bind()
        fun clear()
    }

    abstract class Buffer (
        var width: Int,
        var height: Int
    ) {
        abstract val texture: Texture?

        abstract fun init()
        abstract fun updateDimensions()

        fun resizeToWindow(window: Window) {
            width = window.width
            height = window.height
            updateDimensions()
        }

        fun destroy() {
            texture?.destroy()
        }
    }
}