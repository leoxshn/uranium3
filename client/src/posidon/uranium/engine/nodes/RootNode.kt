package posidon.uranium.engine.nodes

import posidon.uranium.engine.graphics.Renderer
import posidon.uranium.engine.nodes.spatial.Camera

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