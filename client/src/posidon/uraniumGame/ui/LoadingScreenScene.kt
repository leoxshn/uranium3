package posidon.uraniumGame.ui

import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.ui.Gravity
import posidon.uranium.nodes.ui.View
import posidon.uranium.nodes.ui.text.TextLine

class LoadingScreenScene : Scene("loading") {

    val text = TextLine("component", Font()).apply {
        string = "Loading..."
        size.set(View.MATCH_PARENT, 21)
        gravity = Gravity.CENTER
    }

    init {
        add(text)
    }
}