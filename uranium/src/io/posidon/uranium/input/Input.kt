package io.posidon.uranium.input

import io.posidon.library.types.Vec2f
import io.posidon.library.util.set
import io.posidon.uranium.events.KeyPressedEvent
import io.posidon.uranium.events.MouseButtonPressedEvent
import io.posidon.uranium.events.MouseMovedEvent
import io.posidon.uranium.events.ScrollEvent
import io.posidon.uranium.graphics.Window
import io.posidon.uranium.nodes.Scene
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryUtil
import java.nio.IntBuffer

object Input {

    const val PRESS = GLFW.GLFW_PRESS
    const val RELEASE = GLFW.GLFW_RELEASE
    const val REPEAT = GLFW.GLFW_REPEAT

    private var oldCurX = 0.0
    private var oldCurY = 0.0

    private lateinit var keys: IntBuffer
    private lateinit var mouseButtons: IntBuffer

    internal var curX = 0.0
    internal var curY = 0.0

    fun isKeyDown(key: Int): Boolean = keys[key] != RELEASE
    fun isButtonDown(btn: Int): Boolean = mouseButtons[btn] != RELEASE

    internal fun init(windowId: Long) {
        keys = MemoryUtil.memCallocInt(GLFW.GLFW_KEY_LAST)
        mouseButtons = MemoryUtil.memCallocInt(GLFW.GLFW_MOUSE_BUTTON_LAST)
        GLFW.glfwSetKeyCallback(windowId, Input::onKeyPressed)
        GLFW.glfwSetCursorPosCallback(windowId, Input::onMouseMove)
        GLFW.glfwSetMouseButtonCallback(windowId, Input::onMouseButtonPress)
        GLFW.glfwSetScrollCallback(windowId, Input::onScroll)
    }

    internal fun destroy(windowId: Long) {
        GLFW.glfwSetKeyCallback(windowId, null)?.free()
        GLFW.glfwSetCursorPosCallback(windowId, null)?.free()
        GLFW.glfwSetMouseButtonCallback(windowId, null)?.free()
        GLFW.glfwSetScrollCallback(windowId, null)?.free()
        MemoryUtil.memFree(keys)
        MemoryUtil.memFree(mouseButtons)
    }

    private fun onKeyPressed(windowId: Long, key: Int, scanCode: Int, action: Int, mods: Int) {
        if (key != GLFW.GLFW_KEY_UNKNOWN) {
            keys[key] = action
            val window = Window.getByHandle(windowId)!!
            Scene.passEvent(KeyPressedEvent(System.currentTimeMillis(), window, key, action))
        }
    }

    private fun onMouseButtonPress(windowId: Long, btn: Int, action: Int, mods: Int) {
        mouseButtons[btn] = action
        val window = Window.getByHandle(windowId)!!
        Scene.passEvent(MouseButtonPressedEvent(System.currentTimeMillis(), window, btn, action, Vec2f(curX.toFloat(), curY.toFloat())))
    }

    private fun onScroll(windowId: Long, x: Double, y: Double) {
        val window = Window.getByHandle(windowId)!!
        Scene.passEvent(ScrollEvent(System.currentTimeMillis(), window, x, y))
    }

    private fun onMouseMove(windowId: Long, x: Double, y: Double) {
        val window = Window.getByHandle(windowId)!!
        if (window.mouseLocked) {
            curX = x
            curY = y

            val dx = (curX - oldCurX).toFloat()
            val dy = (curY - oldCurY).toFloat()

            Scene.passEvent(MouseMovedEvent(
                System.currentTimeMillis(),
                window,
                Vec2f(x.toFloat(), y.toFloat()),
                Vec2f(dx, dy)
            ))

            oldCurX = curX
            oldCurY = curY
        }
    }
}