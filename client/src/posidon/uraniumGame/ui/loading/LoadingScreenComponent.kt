package posidon.uraniumGame.ui.loading

import posidon.library.types.Vec3f
import posidon.uranium.nodes.ui.UIComponent

class LoadingScreenComponent(name: String) : UIComponent(name) {

    init {
        transform.position.set(0, 0)
        transform.size.set(MATCH_PARENT, MATCH_PARENT)
        transform.keepAspectRatio = true
        setBackgroundPath("res/textures/ui/loading.png")
    }
}