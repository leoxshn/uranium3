package posidon.uranium.input

import org.lwjgl.glfw.*
import posidon.library.types.Vec2f
import posidon.uranium.graphics.Window
import posidon.uranium.events.KeyPressedEvent
import posidon.uranium.events.MouseButtonPressedEvent
import posidon.uranium.events.MouseMovedEvent
import posidon.uranium.events.ScrollEvent
import posidon.uranium.nodes.Scene

object Input {

    const val PRESS = GLFW.GLFW_PRESS
    const val RELEASE = GLFW.GLFW_RELEASE
    const val REPEAT = GLFW.GLFW_REPEAT

    private var oldCurX = 0.0
    private var oldCurY = 0.0

    private val keys = BooleanArray(GLFW.GLFW_KEY_LAST)
    private val mouseButtons = BooleanArray(GLFW.GLFW_MOUSE_BUTTON_LAST)

    internal var curX = 0.0
    internal var curY = 0.0

    fun isKeyDown(key: Int): Boolean = keys[key]
    fun isButtonDown(btn: Int): Boolean = mouseButtons[btn]

    internal fun onKeyPressed(window: Long, key: Int, scanCode: Int, action: Int, mods: Int) {
        keys[key] = action != GLFW.GLFW_RELEASE
        Scene.passEvent(KeyPressedEvent(System.currentTimeMillis(), key, action))
    }

    internal fun onMouseButtonPress(window: Long, btn: Int, action: Int, mods: Int) {
        mouseButtons[btn] = action != GLFW.GLFW_RELEASE
        Scene.passEvent(MouseButtonPressedEvent(System.currentTimeMillis(), btn, action))
    }

    internal fun onScroll(window: Long, x: Double, y: Double) {
        Scene.passEvent(ScrollEvent(System.currentTimeMillis(), x, y))
    }

    internal fun onMouseMove(window: Long, x: Double, y: Double) {
        if (Window.mouseLocked) {
            curX = x
            curY = y

            val dx = (curX - oldCurX).toFloat()
            val dy = (curY - oldCurY).toFloat()

            Scene.passEvent(MouseMovedEvent(
                System.currentTimeMillis(),
                Vec2f(x.toFloat(), y.toFloat()),
                Vec2f(dx, dy)
            ))

            oldCurX = curX
            oldCurY = curY
        }
    }
}