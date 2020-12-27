package posidon.uranium.nodes

import posidon.uranium.events.Event
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.spatial.Eye

abstract class Node {

    var parent: Node? = null
        private set

    /**
     * Adds a [node] as this nodes child.
     * @throws Exception if there's another child with the same name
     */
    fun add(node: Node) {
        children.add(node)
        node.parent = this
    }

    /**
     * Removes [node] from this node's children
     */
    fun remove(node: Node) {
        node.parent = null
        children.remove(node)
    }

    /**
     * Removes all of this node's children
     */
    fun removeAllChildren() {
        for (child in children) child.parent = null
        children.clear()
    }

    fun passEvent(event: Event) {
        onEvent(event)
        for (child in children)
            child.passEvent(event)
    }

    protected open fun onEvent(event: Event) {}
    open fun update(delta: Double) {}
    open fun render(renderer: Renderer, eye: Eye) {}
    open fun destroy() {}

    protected val children = ArrayList<Node>()

    internal fun allChildren(fn: Node.() -> Unit) {
        fn()
        for (child in children) {
            child.allChildren(fn)
        }
    }

    operator fun iterator() = children.iterator()
}