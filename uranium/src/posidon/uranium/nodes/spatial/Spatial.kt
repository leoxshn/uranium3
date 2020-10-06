package posidon.uranium.nodes.spatial

import posidon.library.types.Vec3f
import posidon.uranium.nodes.Node

open class Spatial(
    name: String
) : Node(name) {

    val transform = Transform()

    inline val position: Vec3f get() = transform.position

    val globalTransform: Transform
        get() = if (parent is Spatial) {
            val parentGlobalTransform = (parent as Spatial).globalTransform
            Transform(position = parentGlobalTransform.position + transform.position)
        } else transform
}