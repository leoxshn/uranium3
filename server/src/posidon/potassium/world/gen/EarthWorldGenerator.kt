package posidon.potassium.world.gen

import posidon.potassium.content.Block
import posidon.potassium.content.Material
import posidon.potassium.tools.OpenSimplexNoise
import posidon.potassium.world.Chunk
import kotlin.math.max
import kotlin.math.min

class EarthWorldGenerator(seed: Long) : WorldGenerator() {

    val heightGenHuge = OpenSimplexNoise(seed)
    val heightGenMedium = OpenSimplexNoise(seed - 1)
    val heightGenSmall = OpenSimplexNoise(seed - 3)
    val mountanityGen = OpenSimplexNoise(seed / 2)
    val otherGen = OpenSimplexNoise(seed / 2 + 5)

    override fun genChunk(chunkX: Int, chunkY: Int, chunkZ: Int): Chunk {
        val chunk = Chunk(chunkX, chunkY, chunkZ)
        /*for (x in 0 until Chunk.SIZE) for (y in 0 until Chunk.SIZE) for (z in 0 until Chunk.SIZE) chunk[x, y, z] =
            Block(Material.STONE)*/
        val absX = chunkX * Chunk.SIZE
        val absZ = chunkZ * Chunk.SIZE

        for (x in 0 until Chunk.SIZE) for (z in 0 until Chunk.SIZE) {
            val heightHuge = heightGenHuge.eval((absX + x).toDouble() / 128, (absZ + z).toDouble() / 128)
            val heightMedium = heightGenMedium.eval((absX + x).toDouble() / 64, (absZ + z).toDouble() / 64)
            val heightSmall = heightGenSmall.eval((absX + x).toDouble() / 10, (absZ + z).toDouble() / 10)
            val mountanity = mountanityGen.eval((absX + x).toDouble() / 512, (absZ + z).toDouble() / 512)
            val hottity: Double = otherGen.eval((absX + x) / 1670.0, (absZ + z) / 1670.0)
            val plantgrowity: Double = otherGen.eval((absX + x + 34) / 256.0, (absZ + z + 46) / 256.0)
            val height = 20 +
                    heightSmall * (2 + 6 * mountanity) +
                    heightMedium * (5 + 32 * mountanity) +
                    heightHuge * (120 - 70 * mountanity)
            val heightInChunk = max(0, min(height.toInt() - chunkY * Chunk.SIZE, Chunk.SIZE))
            for (y in 0 until heightInChunk)  {
                if (y == heightInChunk - 1) chunk[x, y, z] = Block(Material.GRASS)
                else chunk[x, y, z] = Block(Material.STONE)
            }
        }
        return chunk
    }
}