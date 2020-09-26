package posidon.potassium.world.gen

import posidon.potassium.world.Chunk

abstract class WorldGenerator {
    abstract fun genChunk(chunkX: Int, chunkY: Int, chunkZ: Int): Chunk
}