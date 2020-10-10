package posidon.uranium.nodes

import posidon.uranium.events.Event
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.spatial.Camera

object NodeTree {

    private var root = Node("root")

    /**
     * Replaces the current root with [root]
     */
    fun setRoot(root: Node) {
        this.root.destroy()
        this.root = root
    }

    /**
     * Passes an [event] to all nodes.
     * Events are handled in the [Node.onEvent] function
     */
    fun passEvent(event: Event) = root.passEvent(event)

    internal fun update(delta: Double) = root.allChildren {
        this.update(delta)
    }

    internal fun render(renderer: Renderer, camera: Camera) = root.allChildren {
        this.render(renderer, camera)
    }

    internal fun destroy() = root.allChildren {
        this.destroy()
    }
}