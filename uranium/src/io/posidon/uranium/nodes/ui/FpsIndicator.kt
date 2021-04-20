package io.posidon.uranium.nodes.ui

import io.posidon.uranium.graphics.Window
import io.posidon.uranium.nodes.FpsCounter
import io.posidon.uranium.nodes.ui.text.MonospaceFont
import io.posidon.uranium.nodes.ui.text.TextLine

class FpsIndicator(window: Window, font: MonospaceFont) : View(window) {

    private val text = TextLine(window, font).apply {
        size.y = MATCH_PARENT
        size.x = WRAP_CONTENT
    }
    private val counter = FpsCounter()

    val color by text::color
    var font by text::font

    init {
        add(text)
        add(counter)
    }

    override fun update(delta: Double) {
        super.update(delta)
        text.string = "FPS: " + counter.fps
    }
}