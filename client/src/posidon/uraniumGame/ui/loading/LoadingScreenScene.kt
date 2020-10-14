package posidon.uraniumGame.ui.loading

import posidon.uranium.nodes.Scene

class LoadingScreenScene : Scene("loading") {

    val component = LoadingScreenComponent("component")

    init {
        add(component)
    }
}