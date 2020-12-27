package posidon.uraniumGame.ui

import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.ui.Gravity
import posidon.uranium.nodes.ui.View
import posidon.uranium.nodes.ui.text.TextLine

class LoadingScreenScene : Scene() {

    val text = TextLine(Font()).apply {
        string = "Loading..."
        size.set(View.MATCH_PARENT, Font.SIZE)
        gravity = Gravity.CENTER
    }

    init {
        add(text)
    }
}