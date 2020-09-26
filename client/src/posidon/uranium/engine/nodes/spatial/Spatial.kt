package posidon.uranium.engine.nodes.spatial

import posidon.library.types.Vec3f
import posidon.uranium.engine.nodes.Node

open class Spatial(
    name: String
) : Node(name) {

    val transform = Transform(Vec3f.zero())

    inline val position get() = transform.position

    val globalTransform: Transform
        get() = if (parent is Spatial) {
            val parentGlobalTransform = (parent as Spatial).globalTransform
            Transform(parentGlobalTransform.position + transform.position)
        } else transform
}