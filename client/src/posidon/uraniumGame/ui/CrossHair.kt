package posidon.uraniumGame.ui

import posidon.uranium.nodes.ui.UIComponent

class CrossHair(name: String) : UIComponent(name) {

    init {
        transform.position.set(0, 0)
        transform.size.set(32, 32)
        transform.keepAspectRatio = false
        setBackgroundPath("res/textures/ui/crosshair.png")
    }
}