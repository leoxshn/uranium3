package posidon.uranium.nodes.spatial

import posidon.library.types.Vec3f
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.Scene

open class Spatial : Node() {

    val transform = Transform()

    inline val position: Vec3f get() = transform.position

    val globalTransform: Transform
        get() = if (parent is Spatial) {
            val parentGlobalTransform = (parent as Spatial).globalTransform
            Transform(position = parentGlobalTransform.position + transform.position)
        } else transform

    fun moveAndSlide(boundingBox: BoundingBox, velocity: Vec3f) {
        val v = velocity.copy()
        val a = v.normalize().apply { selfMultiply(0.1f) }
        while (v.length > 0.1f) {
            v.selfSubtract(a)
            tpAndCollide(boundingBox, a.copy(y = 0f, z = 0f))
            tpAndCollide(boundingBox, a.copy(x = 0f, z = 0f))
            tpAndCollide(boundingBox, a.copy(x = 0f, y = 0f))
        }
        tpAndCollide(boundingBox, v.copy(y = 0f, z = 0f))
        tpAndCollide(boundingBox, v.copy(x = 0f, z = 0f))
        tpAndCollide(boundingBox, v.copy(x = 0f, y = 0f))
    }

    fun isOnSurface(boundingBox: BoundingBox, normal: Vec3f = Vec3f.UP): Boolean {
        val r = normal * 0.1f
        position.selfSubtract(r)
        val c = checkCollide(boundingBox)
        position.selfAdd(r)
        return c
    }

    private fun tpAndCollide(boundingBox: BoundingBox, velocity: Vec3f) {
        position.selfAdd(velocity)
        if (checkCollide(boundingBox)) {
            position.selfSubtract(velocity)
        }
    }

    private fun checkCollide(boundingBox: BoundingBox): Boolean {
        var didCollide = false
        Scene.current.allChildren {
            if (boundingBox != this && this is Collider && collide(boundingBox)) {
                didCollide = true
                return@allChildren
            }
        }
        return didCollide
    }
}