package io.posidon.uranium.nodes

import io.posidon.uranium.events.Event
import io.posidon.uranium.graphics.Filter
import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.nodes.spatial.Eye

abstract class Scene : Node() {

    open fun onSet() {}

    companion object {

        internal var current: Scene = object : Scene() {}
            private set

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

        internal fun update(delta: Double) {
            current.allChildren {
                this.update(delta)
            }
        }

        internal fun render(renderer: Renderer, eye: Eye) {
            current.allChildren {
                this.render(renderer, eye)
            }
        }

        internal fun destroy() {
            current.allChildren {
                this.destroy()
            }
        }

        internal fun nextBuffer(cur: Filter?, renderer: Renderer): Renderer.FrameBuffer {
            val next = (cur?.let {
                current.children.subList(current.children.indexOf(cur) + 1, current.children.lastIndex)
            } ?: current.children).firstOrNull {
                it is Filter && it.enabled
            } as Filter? ?: renderer.window
            next.bind()
            return next
        }
    }
}