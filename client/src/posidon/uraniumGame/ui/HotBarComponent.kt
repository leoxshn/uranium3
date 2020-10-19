package posidon.uraniumGame.ui

import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.ui.UIComponent

class HotBarComponent(name: String) : UIComponent(name) {

    init {
        transform.position.set(0, 0)
        transform.size.set(MATCH_PARENT, 23)
        transform.keepAspectRatio
        setBackgroundPath("res/textures/ui/hotbar.png")
    }

    override fun calculateLight() = Scene.environment.ambientLight
}