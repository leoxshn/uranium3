package posidon.uranium.engine.nodes

import posidon.uranium.engine.graphics.Renderer
import posidon.uranium.engine.input.events.Event
import posidon.uranium.engine.nodes.spatial.Camera

abstract class Node(
    val name: String
) {

    val children: List<Node> by ::_children
    var parent: Node? = null
        private set

    fun add(node: Node) {
        if (children.find { it.name == node.name } != null) {
            throw Exception("A node with the name \"${node.name}\" already exists in \"$name\"")
        }
        _children.add(node)
        node.parent = this
    }

    fun remove(node: Node) {
        node.parent = null
        _children.remove(node)
    }

    fun removeAllChildren() {
        for (child in _children) child.parent = null
        _children.clear()
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

    open fun onEvent(event: Event) {}
    open fun update(delta: Double) {}
    open fun render(renderer: Renderer, camera: Camera) {}
    open fun destroy() {}

    private val _children = ArrayList<Node>()

    protected fun allChildren(fn: Node.() -> Unit) {
        for (child in children) {
            child.fn()
            child.allChildren(fn)
        }
    }
}