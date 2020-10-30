package posidon.uranium.nodes

import posidon.uranium.graphics.Renderer
import posidon.uranium.events.Event
import posidon.uranium.nodes.spatial.Eye

abstract class Node(
    val name: String
) {

    var parent: Node? = null
        private set

    /**
     * Adds a [node] as this nodes child.
     * @throws Exception if there's another child with the same name
     */
    fun add(node: Node) {
        if (children.find { it.name == node.name } != null) {
            throw Exception("A node with the name \"${node.name}\" already exists in \"$name\"")
        }
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

    operator fun get(path: String): Node? {
        val p = path.split('/')
        return children.find { it.name == p[0] }?.let {
            if (p.size == 1) it else it[path.substringAfter('/')]
        }
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

    private val children = ArrayList<Node>()

    internal fun allChildren(fn: Node.() -> Unit) {
        fn()
        for (child in children) {
            child.allChildren(fn)
        }
    }

    operator fun iterator() = children.iterator()
}