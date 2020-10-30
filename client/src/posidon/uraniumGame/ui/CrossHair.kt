package posidon.uraniumGame.ui

import posidon.uranium.nodes.ui.View

class CrossHair(name: String) : View(name) {

    init {
        transform.position.set(0, 0)
        transform.size.set(32, 32)
        transform.keepAspectRatio = false
        setBackgroundPath("res/textures/ui/crosshair.png")
    }
}