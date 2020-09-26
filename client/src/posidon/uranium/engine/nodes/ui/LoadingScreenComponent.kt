package posidon.uranium.engine.nodes.ui

import posidon.library.types.Vec3f

class LoadingScreenComponent(name: String) : UIComponent(name) {

    init {
        transform.position.set(0, 0)
        transform.size.set(MATCH_PARENT, MATCH_PARENT)
        transform.keepAspectRatio = true
        setBackgroundPath("res/textures/ui/loading.png")
    }

    val light = Vec3f(1f, 1f, 1f)
    override fun calculateLight() = light
}