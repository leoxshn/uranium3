package posidon.uranium.input

import org.lwjgl.glfw.*
import posidon.library.types.Vec2f
import posidon.uranium.graphics.Window
import posidon.uranium.input.events.MouseMovedEvent
import posidon.uranium.nodes.RootNode

class Input(w: Window) {

    private var oldCurX = 0.0
    private var oldCurY = 0.0

    companion object {
        private val keys = BooleanArray(GLFW.GLFW_KEY_LAST)
        private val mouseButtons = BooleanArray(GLFW.GLFW_MOUSE_BUTTON_LAST)
        var curX = 0.0
        var curY = 0.0
        var scrollX = 0.0
        var scrollY = 0.0
        fun isKeyDown(key: Int): Boolean = keys[key]
        fun isButtonDown(btn: Int): Boolean = mouseButtons[btn]
    }

    fun onKeyPressed(window: Long, key: Int, scanCode: Int, action: Int, mods: Int) {
        keys[key] = action != GLFW.GLFW_RELEASE
        when (key) {
            Key.F11 -> Window.isFullscreen = !Window.isFullscreen
            Key.ESCAPE -> Window.mouseLocked = false
        }
    }

    fun onMouseButtonPress(window: Long, btn: Int, action: Int, mods: Int) {
        mouseButtons[btn] = action != GLFW.GLFW_RELEASE
        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) Window.mouseLocked = true
    }

    fun onScroll(window: Long, x: Double, y: Double) {
        scrollX += x
        scrollY += y
    }

    fun onMouseMove(window: Long, x: Double, y: Double) {
        if (Window.mouseLocked) {
            curX = x
            curY = y

            val dx = (curX - oldCurX).toFloat()
            val dy = (curY - oldCurY).toFloat()

            RootNode.passEvent(MouseMovedEvent(
                Vec2f(x.toFloat(), y.toFloat()),
                Vec2f(dx, dy)
            ))

            oldCurX = curX
            oldCurY = curY
        }
    }
}