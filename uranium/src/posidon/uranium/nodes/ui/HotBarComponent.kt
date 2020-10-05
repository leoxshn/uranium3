package posidon.uranium.nodes.ui

class HotBarComponent(name: String) : UIComponent(name) {

    init {
        transform.position.set(0, 0)
        transform.size.set(MATCH_PARENT, 23)
        transform.keepAspectRatio
        setBackgroundPath("res/textures/ui/hotbar.png")
    }
}