package io.posidon.uranium.graphics

import io.posidon.uranium.Uranium
import io.posidon.uranium.events.WindowResizedEvent
import io.posidon.uranium.input.Input
import io.posidon.uranium.nodes.Scene
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.*
import java.util.*

class Window private constructor() : Renderer.FrameBuffer {

    var width = 0; private set
    var height = 0; private set

    /**
     * The aspect ratio of the display (width:height)
     */
    inline val aspectRatio: Float
        get() = width.toFloat() / height

    /**
     * Sets window title
     */
    var title: String = "uranium"
        set(value) {
            field = value
            GLFW.glfwSetWindowTitle(id, field)
        }

    /**
     * This is pretty self-explanatory, isn't it?
     */
    val isOpen get() = !GLFW.glfwWindowShouldClose(id)

    /**
     * Whether the cursor is in it's normal state, or locked to the window and invisible
     */
    var mouseLocked = false
        set(value) {
            if (value) GLFW.glfwSetCursorPos(id, Input.curX, Input.curY)
            field = value
            GLFW.glfwSetInputMode(id, GLFW.GLFW_CURSOR, if (value) GLFW.GLFW_CURSOR_DISABLED else GLFW.GLFW_CURSOR_NORMAL)
        }

    /**
     * Sets the window to be fullscreen or not
     */
    var isFullscreen = false
        set(value) {
            field = value
            if (value) {
                GLFW.glfwMaximizeWindow(id)
            } else {
                GLFW.glfwRestoreWindow(id)
            }
        }

    val renderer = Renderer.get(this)

    private var id: Long = 0

    companion object {
        fun init() {
            check(GLFW.glfwInit()) {
                "[GLFW ERROR]: GLFW wasn't inititalized"
            }
            GLFWErrorCallback.createPrint().set()
        }

        private val windows = LinkedList<Window>()

        fun new(): Window = Window().also { windows.add(it) }

        internal fun getByHandle(id: Long): Window? = windows.find { it.id == id }
    }

    fun init(width: Int, height: Int) {
        this.width = width
        this.height = height

        renderer.preWindowInit()

        id = GLFW.glfwCreateWindow(this.width, this.height, title, 0, 0)
        if (id == 0L) {
            System.err.println("[GLFW ERROR]: Window wasn't created")
            return
        }
        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        GLFW.glfwSetWindowPos(id, (videoMode!!.width() - this.width) / 2, (videoMode.height() - this.height) / 2)
        GLFW.glfwSetWindowSizeLimits(id, 600, 300, -1, -1)
        GLFW.glfwMakeContextCurrent(id)
        GL.createCapabilities()
        Input.init(id)
        GLFW.glfwSetWindowSizeCallback(id) { _: Long, w: Int, h: Int ->
            val event = WindowResizedEvent(System.currentTimeMillis(), this, this.width, this.height, w, h)
            this.width = w
            this.height = h
            GL11.glViewport(0, 0, this.width, this.height)
            Scene.passEvent(event)
        }
        GLFW.glfwShowWindow(id)
        GLFW.glfwSwapInterval(1)
        renderer.init(id)
    }

    fun loop() {
        while (isOpen && Uranium.running) {
            renderer.executeEventsFromQueue()
            //Scene.environment.sun?.renderShadowMap(renderer)
            Scene.nextBuffer(null, renderer)
            renderer.eye?.let { Scene.render(renderer, it) }
            swapBuffers()
            update()
        }
        Uranium.end()
    }

    internal fun update() {
        clear()
        GLFW.glfwPollEvents()
    }

    internal fun swapBuffers() = GLFW.glfwSwapBuffers(id)

    fun destroy() {
        renderer.destroy()
        Input.destroy(id)
        GLFW.glfwSetWindowSizeCallback(id, null)?.free()
        GLFW.glfwDestroyWindow(id)
        GLFW.glfwTerminate()
        windows.remove(this)
    }

    override fun bind() {
        GL30C.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
        GL11C.glViewport(0, 0, width, height)
    }

    override fun clear() {
        GL11C.glClearColor(0f, 0f, 0f, 1.0f)
        GL11C.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Window

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}