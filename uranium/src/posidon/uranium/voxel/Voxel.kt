package posidon.uranium.voxel

import posidon.library.types.Vec2f
import posidon.library.types.Vec3i

abstract class Voxel(
    val id: String,
    var posInChunk: Vec3i,
    val chunk: VoxelChunk<*>
) {

    inline val chunkPos get() = chunk.position
    inline val absolutePosition inline get() = chunk.absolutePosition.apply { selfAdd(posInChunk) }

    abstract fun getUV(): Vec2f
}