package posidon.uranium.nodes

import posidon.uranium.events.Event
import posidon.uranium.graphics.Filter
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Window
import posidon.uranium.nodes.environment.Environment
import posidon.uranium.nodes.environment.NullEnvironment
import posidon.uranium.nodes.spatial.Eye

abstract class Scene(
    name: String,
    val environment: Environment = NullEnvironment()
) : Node(name) {

    final override fun update(delta: Double) {
        environment.update(delta)
        Window.backgroundColor.set(environment.skyColor)
    }

    override fun onEvent(event: Event) {
        environment.onEvent(event)
    }

    override fun destroy() {
        environment.destroy()
    }

    open fun onSet() {}

    companion object {

        internal var current: Scene = object : Scene("") {}
            private set

        operator fun get(path: String) = current[path]

        /**
         * The object handling atmosphere-related stuff
         */
        val environment get() = current.environment

        /**
         * Replaces the current root with [root]
         */
        fun set(scene: Scene) {
            this.current = scene
            scene.onSet()
        }

        /**
         * Passes an [event] to all nodes.
         * Events are handled in the [Node.onEvent] function
         */
        fun passEvent(event: Event) = current.passEvent(event)

        internal fun update(delta: Double) = current.allChildren {
            this.update(delta)
        }

        internal fun render(renderer: Renderer, eye: Eye) = current.allChildren {
            this.render(renderer, eye)
        }

        internal fun destroy() = current.allChildren {
            this.destroy()
        }

        internal fun nextBuffer(cur: Filter?): Renderer.FrameBuffer {
            val next = (cur?.let {
                current.children.subList(current.children.indexOf(cur) + 1, current.children.lastIndex)
            } ?: current.children).firstOrNull {
                it is Filter && it.enabled
            } as Filter? ?: Window
            next.bind()
            return next
        }
    }
}