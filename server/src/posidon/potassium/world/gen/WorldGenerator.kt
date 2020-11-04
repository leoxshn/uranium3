package posidon.potassium.world.gen

import posidon.library.types.Vec3i
import posidon.potassium.world.Chunk

abstract class WorldGenerator {
    abstract fun genChunk(chunkPos: Vec3i): Chunk
    open fun clearCache() {}
}