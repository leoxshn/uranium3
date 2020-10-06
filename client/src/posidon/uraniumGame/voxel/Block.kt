package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import java.util.*

class Block(
    val id: String,
    var posInChunk: Vec3i,
    var chunkPos: Vec3i,
    val chunk: Chunk
) {

    inline val absolutePosition inline get() = chunkPos * Chunk.SIZE + posInChunk

    override fun hashCode() = Objects.hash(chunkPos, posInChunk, id)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Block
        if (id != other.id) return false
        if (chunkPos != other.chunkPos) return false
        if (posInChunk != other.posInChunk) return false
        return true
    }
}