package posidon.uranium.voxel

import posidon.library.types.Vec2f
import posidon.library.types.Vec3i

abstract class Voxel(
    val id: String
) {
    abstract fun getUV(): Vec2f
}