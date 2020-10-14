package posidon.uranium.graphics

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import posidon.library.types.Vec3f
import posidon.uranium.events.WindowResizedEvent
import posidon.uranium.input.Input
import posidon.uranium.nodes.Scene

object Window {

    private var id: Long = 0

    var width = 0; private set
    var height = 0; private set

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
        set(fullscreen) {
            if (fullscreen) {
                GLFW.glfwGetWindowPos(id, intArrayOf(pos[0]), intArrayOf(pos[1]))
                val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
                GLFW.glfwSetWindowMonitor(id, GLFW.glfwGetPrimaryMonitor(), 0, 0, videoMode!!.width(), videoMode.height(), 0)
            } else GLFW.glfwSetWindowMonitor(id, 0, pos[0], pos[1], width, height, 0)
            field = fullscreen
        }

    /**
     * Background color of the framebuffer
     */
    val backgroundColor = Vec3f(0f, 0f, 0f)

    private val pos = IntArray(2)

    internal fun init(width: Int, height: Int) {
        Window.width = width
        Window.height = height

        if (!GLFW.glfwInit()) {
            System.err.println("[GLFW ERROR]: GLFW wasn't inititalized")
            return
        }
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE)

        // Antialiasing
        GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 4)
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4)

        id = GLFW.glfwCreateWindow(Window.width, Window.height, title, 0, 0)
        if (id == 0L) {
            System.err.println("[GLFW ERROR]: Window wasn't created")
            return
        }
        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        pos[0] = (videoMode!!.width() - Window.width) / 2
        pos[1] = (videoMode.height() - Window.height) / 2
        GLFW.glfwSetWindowPos(id, pos[0], pos[1])
        GLFW.glfwSetWindowSizeLimits(id, 600, 300, -1, -1)
        GLFW.glfwMakeContextCurrent(id)
        GL.createCapabilities()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        createCallbacks()
        GLFW.glfwShowWindow(id)
        GLFW.glfwSwapInterval(1)
    }

    private fun createCallbacks() {
        GLFW.glfwSetKeyCallback(id, Input::onKeyPressed)
        GLFW.glfwSetCursorPosCallback(id, Input::onMouseMove)
        GLFW.glfwSetMouseButtonCallback(id, Input::onMouseButtonPress)
        GLFW.glfwSetScrollCallback(id, Input::onScroll)
        GLFW.glfwSetWindowSizeCallback(id) { _: Long, w: Int, h: Int ->
            val event = WindowResizedEvent(System.currentTimeMillis(), width, height, w, h)
            width = w
            height = h
            GL11.glViewport(0, 0, width, height)
            Scene.passEvent(event)
        }
    }

    internal fun update() {
        GL11.glClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z, 1.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
        GLFW.glfwPollEvents()
    }

    internal fun swapBuffers() = GLFW.glfwSwapBuffers(id)

    internal fun kill() {
        GLFW.glfwSetKeyCallback(id, null)?.free()
        GLFW.glfwSetCursorPosCallback(id, null)?.free()
        GLFW.glfwSetMouseButtonCallback(id, null)?.free()
        GLFW.glfwSetScrollCallback(id, null)?.free()
        GLFW.glfwSetWindowSizeCallback(id, null)?.free()
        GLFW.glfwDestroyWindow(id)
        GLFW.glfwTerminate()
    }
}