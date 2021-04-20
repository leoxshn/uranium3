package io.posidon.uranium.nodes.ui

import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.graphics.Window
import org.lwjgl.opengl.GL11

class CrossHair(window: Window) : View(window) {

    init {
        transform.keepAspectRatio = true
        gravity = Gravity.CENTER
        transform.translation.set(0, 0)
        transform.size.set(32, 32)
    }

    override fun preRender() {
        GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR)
    }

    override fun postRender(renderer: Renderer) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }
}