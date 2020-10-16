package posidon.uranium.nodes

import posidon.uranium.events.Event
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Window
import posidon.uranium.nodes.environment.Environment
import posidon.uranium.nodes.environment.NullEnvironment
import posidon.uranium.nodes.spatial.Camera

abstract class Scene(name: String) : Node(name) {

    open val environment: Environment = NullEnvironment

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

    companion object {

        private var currentScene: Scene = object : Scene("") {}

        operator fun get(path: String) = currentScene[path]

        /**
         * The object handling atmosphere-related stuff
         */
        val environment get() = currentScene.environment

        /**
         * Replaces the current root with [root]
         */
        fun set(scene: Scene) {
            this.currentScene.destroy()
            this.currentScene = scene
        }

        /**
         * Passes an [event] to all nodes.
         * Events are handled in the [Node.onEvent] function
         */
        fun passEvent(event: Event) = currentScene.passEvent(event)

        internal fun update(delta: Double) = currentScene.allChildren {
            this.update(delta)
        }

        internal fun render(renderer: Renderer, camera: Camera) = currentScene.allChildren {
            this.render(renderer, camera)
        }

        internal fun destroy() = currentScene.allChildren {
            this.destroy()
        }
    }
}