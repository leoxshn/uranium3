package posidon.uranium.nodes.ui

import posidon.uranium.nodes.FpsCounter
import posidon.uranium.nodes.ui.text.MonospaceFont
import posidon.uranium.nodes.ui.text.TextLine

class FpsIndicator(name: String, font: MonospaceFont) : View(name) {

    private val text = TextLine("text", font)
    private val counter = FpsCounter("counter")

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