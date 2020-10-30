package posidon.uraniumGame.ui

import posidon.uranium.nodes.ui.Gravity
import posidon.uranium.nodes.ui.View

class CrossHair(name: String) : View(name) {

    init {
        transform.translation.set(0, 0)
        transform.size.set(32, 32)
        transform.keepAspectRatio = true
        gravity = Gravity.CENTER
        setBackgroundPath("res/textures/ui/crosshair.png")
    }
}