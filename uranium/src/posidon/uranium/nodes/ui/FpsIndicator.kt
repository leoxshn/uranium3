package posidon.uranium.nodes.ui

import posidon.uranium.nodes.FpsCounter
import posidon.uranium.nodes.ui.text.MonospaceFont
import posidon.uranium.nodes.ui.text.TextLine

class FpsIndicator(font: MonospaceFont) : View() {

    private val text = TextLine(font).apply {
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