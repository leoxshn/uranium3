package posidon.potassium.world.gen

import posidon.library.types.Vec2i
import posidon.library.types.Vec3i
import posidon.potassium.content.Block
import posidon.potassium.content.Material
import posidon.potassium.tools.OpenSimplexNoise
import posidon.potassium.world.Chunk
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class EarthWorldGenerator(seed: Long) : WorldGenerator() {

    private val openSimplexNoise = OpenSimplexNoise(seed)

    private val heightsAndFlatnesses = HashMap<Vec2i, Pair<Double, Double>>()

    private inline fun getHeightAndFlatness(absX: Double, absZ: Double) = heightsAndFlatnesses.getOrPut(Vec2i(absX.toInt(), absZ.toInt())) {
        val flatness = run {
            val meta = openSimplexNoise.get(absX, absZ, scale = 192, offset = 100).pow(2)

            val a = run {
                val a = openSimplexNoise.eval((absX + 300) / 192, (absZ + 300) / 192)
                when {
                    a > 0.0 -> (sqrt(a) + 1) / 2.0
                    a < 0.0 -> (-sqrt(-a) + 1) / 2.0
                    else -> 0.0
                }
            }
            val b = openSimplexNoise.get(absX, absZ, scale = 128, offset = 80)

            a * meta + b * (1.0 - meta)
        }

        val mountainHeight = run {
            val pointiness = 1.0 + openSimplexNoise.get(absX + 100, absZ + 100, scale = 84)
            val h = openSimplexNoise.get(absX, absZ, scale = 256).pow(pointiness) * pointiness
            val avgHeight = run {
                val a = openSimplexNoise.get(absX + 56, absZ + 56, scale = 256)
                val b = openSimplexNoise.get(absX + 56, absZ - 56, scale = 256)
                val c = openSimplexNoise.get(absX - 56, absZ + 56, scale = 256)
                val d = openSimplexNoise.get(absX - 56, absZ - 56, scale = 256)
                (a + b + c + d + h) / 5.0
            }
            avgHeight * flatness + h * (1.0 - flatness)
        }

        val heightMedium = openSimplexNoise.get(absX, absZ, scale = 64)
        val heightSmall = openSimplexNoise.get(absX, absZ, scale = 10)
        val height = 20 + heightSmall * (6 * (1 - flatness)) +
            heightMedium * (24 * (1 - flatness)) +
            mountainHeight * (50 + 70 * (1 - flatness))

        height to flatness
    }

    private fun genVoxel(absX: Double, absY: Double, absZ: Double, height: Double, flatness: Double): Boolean {
        val heightF = max(min(5f, (height.toFloat() - absY.toFloat()) / 8f), -5f)

        val mediumBlockF = openSimplexNoise.get(absX, absY, absZ, scale = 16, offset = 20, convertToMin0Max1 = false)
        val bigBlockF = openSimplexNoise.get(absX, absY, absZ, scale = 32, offset = 704, convertToMin0Max1 = false)

        val craziness = openSimplexNoise.get(absX, absY, absZ, scale = 256, offset = 74).pow(1.6)

        val blockF = heightF +
            mediumBlockF * (2f * craziness * (1 - flatness)) +
            bigBlockF * (5f * craziness)

        return blockF > 0.5
    }

    override fun genChunk(chunkPos: Vec3i): Chunk {
        val chunk = Chunk(chunkPos)
        val absChunkX = chunkPos.x * Chunk.SIZE
        val absChunkZ = chunkPos.z * Chunk.SIZE

        for (x in 0 until Chunk.SIZE) for (z in 0 until Chunk.SIZE) {

            val absX = (absChunkX + x).toDouble()
            val absZ = (absChunkZ + z).toDouble()

            val (height, flatness) = getHeightAndFlatness(absX, absZ)

            val absChunkY = chunkPos.y * Chunk.SIZE

            for (y in 0 until Chunk.SIZE) {
                val absY = (absChunkY + y).toDouble()
                if (genVoxel(absX, absY, absZ, height, flatness)) {
                    if (y == Chunk.SIZE - 1) {
                        chunk[x, y, z] = if (!genVoxel(absX, absY + 1, absZ, height, flatness) && chunk[x, y - 1, z] != null) Block(Material.GRASS) else Block(Material.STONE)
                    } else {
                        chunk[x, y, z] = Block(Material.STONE)
                    }
                } else if (y != 0) {
                    val shouldBeGrass = when (y) {
                        1 -> chunk[x, y - 1, z] != null && genVoxel(absX, absY - 2, absZ, height, flatness)
                        else -> chunk[x, y - 1, z] != null && chunk[x, y - 2, z] != null
                    }
                    if (shouldBeGrass) {
                        chunk[x, y - 1, z] = Block(Material.GRASS)
                    }
                }
            }
        }
        return chunk
    }

    override fun clearCache() {
        heightsAndFlatnesses.clear()
    }
}