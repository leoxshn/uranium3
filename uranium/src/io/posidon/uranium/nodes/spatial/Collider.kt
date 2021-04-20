package io.posidon.uranium.nodes.spatial

import io.posidon.library.types.Vec3f

interface Collider {
    fun collide(point: Vec3f): Boolean
    fun collide(boundingBox: BoundingBox): Boolean
}