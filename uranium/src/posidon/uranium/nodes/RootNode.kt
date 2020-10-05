package posidon.uranium.nodes

import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.spatial.Camera

object RootNode : Node("root") {

    fun setScene(node: Node) {
        removeAllChildren()
        add(node)
    }

    override fun update(delta: Double) = allChildren {
        this.update(delta)
    }

    override fun render(renderer: Renderer, camera: Camera) = allChildren {
        this.render(renderer, camera)
    }

    override fun destroy() = allChildren {
        this.destroy()
    }
}