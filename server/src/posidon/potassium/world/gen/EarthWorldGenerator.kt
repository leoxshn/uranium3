package posidon.potassium.world.gen

import posidon.library.types.Vec3i
import posidon.potassium.Console
import posidon.potassium.content.Block
import posidon.potassium.tools.OpenSimplexNoise
import posidon.potassium.tools.OpenSimplexNoiseTileable3D
import posidon.potassium.world.Chunk
import kotlin.math.*

class EarthWorldGenerator(seed: Long, val mapSize: Int, mapHeight: Int) : WorldGenerator() {

    private val openSimplexNoise = OpenSimplexNoise(seed, mapSize)

    private val openSimplexNoise3Dx8 = OpenSimplexNoiseTileable3D(seed, mapSize, mapHeight, 8)
    private val openSimplexNoise3Dx16 = OpenSimplexNoiseTileable3D(seed, mapSize, mapHeight, 16)
    private val openSimplexNoise3Dx24 = OpenSimplexNoiseTileable3D(seed, mapSize, mapHeight, 24)
    private val openSimplexNoise3Dx64 = OpenSimplexNoiseTileable3D(seed, mapSize, mapHeight, 64)

    private val heightsAndFlatnesses = arrayOfNulls<Pair<Double, Double>>(mapSize * mapSize)

    /**
     * Height is in the range [0.0, mapHeight]
     * Flatness is in the range [0.0, 1.0]
     */
    private inline fun getHeightAndFlatness(absX: Double, absZ: Double): Pair<Double, Double> {
        val i = absX.toInt() * mapSize + absZ.toInt()
        return heightsAndFlatnesses[i] ?: run {
            val startTime = System.currentTimeMillis()
            val flatness = run {
                val a = run {
                    val a = openSimplexNoise.tile(absX, absZ, scale = 192, offset = 300.0)
                    when {
                        a > 0.0 -> (sqrt(a) + 1) / 2.0
                        a < 0.0 -> (-sqrt(-a) + 1) / 2.0
                        else -> 0.0
                    }
                }
                val b = ((openSimplexNoise.tile(absX, absZ, scale = 192, offset = 100.0) + 1) / 2).pow(2)
                max(a, b)
            }

            val mountainHeight = ((openSimplexNoise.tile(absX, absZ, scale = 128) + 1) / 2).pow(3)

            val heightMedium = openSimplexNoise.tile(absX, absZ, scale = 48)
            val heightSmall = openSimplexNoise.tile(absX, absZ, scale = 10)
            val height = 96 + heightSmall * (7 * (1 - flatness)) +
                    heightMedium * (24 * (1 - flatness)) +
                    mountainHeight * 192

            val totalTime = System.currentTimeMillis() - startTime
            if (totalTime > 1) Console.beforeCmdLine {
                Console.printInfo(totalTime.toString(), " millis : height")
            }

            height to flatness
        }.also { heightsAndFlatnesses[i] = it }
    }

    private inline fun getReturn(current: Double, maxChange: Double, threshold: Double, ret: (Boolean) -> Unit) {
        if (current - maxChange > threshold) {
            ret(true)
        } else if (current + maxChange <= threshold) {
            ret(false)
        }
    }

    private fun genVoxel(absX: Double, absY: Double, absZ: Double, height: Double, flatness: Double): Boolean {

        val startTime = System.currentTimeMillis()

        if (absY < height - 5.0) return true

        val heightF = max(min(5.0, (height - absY) / 8.0), -5.0)

        val invFlatness = 1 - flatness
        val bigFMultiplier = 1.2 + 2.4 * invFlatness
        val threshold = 0.5

        getReturn(heightF, bigFMultiplier + invFlatness, threshold) { return it }

        val bigF = openSimplexNoise3Dx24.get(absX, absY, absZ, offset = 704.0)

        var blockF = heightF + bigF * bigFMultiplier

        getReturn(blockF, invFlatness, threshold) { return it }

        blockF += openSimplexNoise3Dx16.get(absX, absY, absZ, offset = 20.0) * invFlatness

        val totalTime = System.currentTimeMillis() - startTime
        if (totalTime > 1) Console.beforeCmdLine {
            Console.printInfo(totalTime.toString(), " millis : voxel")
        }

        return blockF > threshold
    }

    private fun getCave(absX: Double, absY: Double, absZ: Double, height: Double, flatness: Double): Boolean {
        if (absY > height) {
            return false
        }

        val threshold = .72
        val taper = when {
            absY < 12 -> sqrt(sqrt(absY / 12))
            absY > height - 20 -> sqrt(sqrt((height - absY) / 20))
            else -> 1.0
        }

        var caveness: Double

        /// RANDOMIZATION
        val emptinessMini = openSimplexNoise3Dx16.get(absX, absY, absZ, offset = 8.0)
        caveness = emptinessMini * .24

        /// HUGE CAVES
        val m = openSimplexNoise3Dx64.get(absX, absY, absZ, offset = 23.0).coerceAtLeast(0.0).pow(0.15)
        caveness += ((openSimplexNoise3Dx24.get(absX, absY, absZ, offset = 8.0) / 2 + .5) * m) *
            (1.6 - absY / 52).coerceAtLeast(0.0).pow(2)

        /// STALACTHINGS
        caveness -= openSimplexNoise3Dx16.get(absX, absY / 24, absZ, offset = 37.0)
            .coerceAtLeast(0.0).times(1.2).pow(12)
        caveness -= sqrt(sqrt(openSimplexNoise3Dx24.get(absX, absY, absZ, offset = 25.0).coerceAtLeast(0.0)) + 1) *
            openSimplexNoise3Dx8.get(absX, absY / 15, absZ, offset = 68.0).times(1.2).pow(10)

        /// WORM CAVES
        caveness += max(
            a = (1 - abs(openSimplexNoise3Dx64.get(absX, absY, absZ))).pow(2) *
                (1 - abs(openSimplexNoise3Dx64.get(absX, absY, absZ, offset = 156.0))).pow(1.5) *
                (1 - abs(openSimplexNoise3Dx64.get(absX, absY, absZ, offset = 272.0))).pow(2),
            b = (1 - abs(openSimplexNoise3Dx24.get(absX, absY, absZ))).pow(1.2) *
                (1 - abs(openSimplexNoise3Dx24.get(absX, absY, absZ, offset = 136.0))).pow(1.8) *
                (1 - abs(openSimplexNoise3Dx64.get(absX, absY, absZ, offset = 36.0))).pow(2)
        ) * if (absY < 12) taper else 1.0

        /// TAPER
        caveness *= taper

        return caveness > threshold
    }

    override fun genChunk(chunkPos: Vec3i): Chunk {
        val chunk = Chunk()
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
                    chunk[x, y, z] = Block.STONE
                }
            }

            for (y in 0 until Chunk.SIZE) {
                val absY = (absChunkY + y).toDouble()
                if (chunk[x, y, z] != null) {
                    if ((if (y == Chunk.SIZE - 1) !genVoxel(absX, absY + 1, absZ, height, flatness) else chunk[x, y + 1, z] == null) &&
                        (if (y == 0) genVoxel(absX, absY - 1, absZ, height, flatness) else chunk[x, y - 1, z] != null)) {
                        chunk[x, y, z] = Block.DIRT
                    }
                }
            }

            for (y in 0 until Chunk.SIZE) {
                val absY = (absChunkY + y).toDouble()
                if (getCave(absX, absY, absZ, height, flatness)) {
                    chunk[x, y, z] = null
                }
            }
        }
        return chunk
    }
}