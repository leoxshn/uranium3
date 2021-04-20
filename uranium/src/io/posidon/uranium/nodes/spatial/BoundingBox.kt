package io.posidon.uranium.nodes.spatial

import io.posidon.library.types.Vec3f

class BoundingBox : Spatial(), Collider {

    var size = Vec3f.zero()
    var centered = true

    override fun collide(point: Vec3f): Boolean {
        val o = getRealOrigin()
        return o.x < point.x && o.x + size.x > point.x &&
            o.y < point.y && o.y + size.y > point.y &&
            o.z < point.z && o.z + size.z > point.z
    }

    override fun collide(boundingBox: BoundingBox): Boolean {
        val min0 = getRealOrigin()
        val min1 = boundingBox.getRealOrigin()
        val max0 = min0 + size
        val max1 = min1 + boundingBox.size

        return min0.x <= max1.x && max0.x >= min1.x &&
            min0.y <= max1.y && max0.y >= min1.y &&
            min0.z <= max1.z && max0.z >= min1.z
    }

    fun getRealOrigin(): Vec3f {
        return if (centered) {
            val p = globalTransform.position.copy()
            p.x -= size.x / 2f
            p.y -= size.y / 2f
            p.z -= size.z / 2f
            p
        } else globalTransform.position
    }
}