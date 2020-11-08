package posidon.uranium.nodes.spatial

import posidon.library.types.Vec3f

interface Collider {
    fun collide(point: Vec3f): Boolean
    fun collide(boundingBox: BoundingBox): Boolean
}