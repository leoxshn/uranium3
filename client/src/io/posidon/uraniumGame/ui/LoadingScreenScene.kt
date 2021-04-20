package io.posidon.uraniumGame.ui

import io.posidon.uranium.graphics.Window
import io.posidon.uranium.nodes.Scene
import io.posidon.uranium.nodes.ui.Gravity
import io.posidon.uranium.nodes.ui.View
import io.posidon.uranium.nodes.ui.text.TextLine

class LoadingScreenScene(window: Window) : Scene() {

    val text = TextLine(window, Font().also { it.create() }).apply {
        string = "Loading..."
        size.set(View.MATCH_PARENT, Font.SIZE)
        gravity = Gravity.CENTER
    }

    init {
        add(text)
    }
}