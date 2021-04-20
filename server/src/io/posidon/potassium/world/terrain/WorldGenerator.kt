package io.posidon.potassium.world.terrain

import io.posidon.potassium.tools.OpenSimplex2STileableXZ
import io.posidon.potassium.world.Chunk
import io.posidon.uraniumPotassium.content.Block
import io.posidon.uraniumPotassium.content.worldGen.Constants.CHUNK_SIZE
import kotlin.math.*
import kotlin.random.Random

class WorldGenerator(seed: Long, val mapSize: Int, mapHeight: Int) {

    private val random = Random(seed)

    private val seedA = random.nextLong()
    private val seedB = random.nextLong()
    private val seedC = random.nextLong()
    private val seedD = random.nextLong()

    private val openSimplex8 = OpenSimplex2STileableXZ(1.0 / 8.0, mapSize)
    private val openSimplex16 = OpenSimplex2STileableXZ(1.0 / 16.0, mapSize)
    private val openSimplex24 = OpenSimplex2STileableXZ(1.0 / 24.0, mapSize)
    private val openSimplex48 = OpenSimplex2STileableXZ(1.0 / 48.0, mapSize)
    private val openSimplex64 = OpenSimplex2STileableXZ(1.0 / 64.0, mapSize)
    private val openSimplex72 = OpenSimplex2STileableXZ(1.0 / 72.0, mapSize)
    private val openSimplex96 = OpenSimplex2STileableXZ(1.0 / 96.0, mapSize)
    private val openSimplex128 = OpenSimplex2STileableXZ(1.0 / 128.0, mapSize)
    private val openSimplex192 = OpenSimplex2STileableXZ(1.0 / 192.0, mapSize)
    private val openSimplex256 = OpenSimplex2STileableXZ(1.0 / 256.0, mapSize)

    private val heightsAndFlatnesses = DoubleArray(mapSize * mapSize * 2) { -1.0 }

    /*var chunkTime = 0L
    var chunkCount = 0
    var caveTime = 0L
    var caveCount = 0
    var voxelTime = 0L
    var voxelCount = 0
    var heightTime = 0L
    var heightCount = 0

    init {
        thread(isDaemon = true) {
            while (running) {
                Thread.sleep(12000)
                if (caveCount != 0 || voxelCount != 0 || heightCount != 0) Console.beforeCmdLine {
                    Console.println(Console.colors.GREEN_BOLD_BRIGHT + "WorldGenerator averages")
                    Console.println("chunk: ${chunkTime / chunkCount.toFloat()} ns")
                    Console.println("cave: ${caveTime / caveCount.toFloat()} ns")
                    Console.println("voxel: ${voxelTime / voxelCount.toFloat()} ns")
                    Console.println("height: ${heightTime / heightCount.toFloat()} ns")
                }
            }
        }
    }*/

    /**
     * Height is in the range [0.0, mapHeight]
     * Flatness is in the range [0.0, 1.0]
     */
    private inline fun getHeightAndFlatness(absX: Double, absZ: Double): Pair<Double, Double> {
        //val startTime = System.currentTimeMillis()
        val i = absX.toInt() * mapSize + absZ.toInt()
        return (if (heightsAndFlatnesses[i * 2] != -1.0) heightsAndFlatnesses[i * 2] to heightsAndFlatnesses[i * 2 + 1] else {
            val terracing = (openSimplex192.get(seedA, absX, absZ, offset = 25.0).pow(0.2) + 1) / 2

            val flatness = run {
                val a = openSimplex192.get(seedA, absX, absZ, offset = 300.0)
                when {
                    a > 0.0 -> (sqrt(a) + 1) / 2.0
                    a < 0.0 -> (-sqrt(-a) + 1) / 2.0
                    else -> 0.0
                }.pow(openSimplex192.get(seedB, absX, absZ) + 1)
            }

            val mountainHeight =
                ((openSimplex128.get(seedC, absX, absZ) + 1) / 2).pow(3) *
                ((openSimplex192.get(seedD, absX, absZ) + 1) / 2).pow(1.2) *
                (1.6 - abs(openSimplex192.get(seedC, absX, absZ))) *
                min(1.0, max(0.2, abs(openSimplex256.get(seedC, absX, absZ)).times(2.0).pow(0.5).times(2.0)))

            var height = 136 + mountainHeight * 256
            val invFlatness = 1 - flatness
            height += openSimplex48.get(seedD, absX, absZ) * (28 * invFlatness)
            height += openSimplex8.get(seedA, absX, absZ) * (7 * invFlatness)

            if (terracing > 0.0) {
                val smoothHeight = height
                val terracingAmount = 12
                height /= terracingAmount
                val ih = round(height)
                height = (ih + 0.5 * (2 * (height - ih)).pow(11)) * terracingAmount
                height = smoothHeight * (1 - terracing) + height * terracing
            }

            heightsAndFlatnesses[i * 2] = height
            heightsAndFlatnesses[i * 2 + 1] = flatness
            height to flatness
        }).also {
            //val totalTime = System.currentTimeMillis() - startTime
            //heightTime += totalTime
            //heightCount++
        }
    }

    private fun genVoxel(absX: Double, absY: Double, absZ: Double, height: Double, flatness: Double): Boolean {
        //val startTime = System.nanoTime()

        if (absY < height - 5.0) return true

        // Height
        var blockF = max(min(5.0, (height - absY) / 8.0), -5.0)

        val invFlatness = 1 - flatness
        val microMultiplier = 0.1 + 0.2 * invFlatness
        val bigFMultiplier = 1.0 + 3.2 * invFlatness
        val overhangMultiplier = 1.0 + 2.8 * invFlatness
        val threshold = 0.5

        blockF += openSimplex48.get(seedA, absX, absY * 1.8, absZ, offset = 12.0) * bigFMultiplier

        // Overhangs
        blockF += (1 - abs(openSimplex48.get(seedB, absX, absY, absZ))).pow(2) * overhangMultiplier

        blockF += openSimplex8.get(seedB, absX, absY, absZ, offset = 24.0) * microMultiplier

        //val totalTime = System.nanoTime() - startTime
        //voxelTime += totalTime
        //voxelCount++

        return blockF > threshold
    }

    private fun getCave(absX: Double, absY: Double, absZ: Double, height: Double, flatness: Double): Boolean {
        //val start = System.nanoTime()

        if (absY > height) {
            return false
        }

        val threshold = .72

        var caveness = 0.0

        /// STALACTHINGS
        var s = openSimplex24.get(seedA, absX, absY, absZ, offset = 25.0)
        if (s > 0.0) {
            s = sqrt(sqrt(s) + 1)
            caveness -= s * openSimplex8.get(seedA, absX, absY / 15, absZ, offset = 68.0).times(1.2).pow(10)
        }

        if (absY > 20 && absY < height - 16 && caveness > threshold) return true

        /// HUGE CAVES
        var m = openSimplex72.get(seedC, absX, absY * 2.0, absZ, offset = 23.0)
        if (m > 0.0) {
            m = m.pow(0.25)
            caveness += (openSimplex48.get(seedB, absX, absY, absZ, offset = 8.0) / 2 + .5) * m *
                (1.6 - absY / max(165.0, height)).coerceAtLeast(0.0).pow(2)
        }

        val taper = when {
            absY < 20 -> sqrt(absY / 20)
            absY > height - 16 -> sqrt(sqrt((height - absY) / 16))
            else -> 1.0
        }

        /// TAPER
        caveness *= taper

        if (caveness > threshold) return true

        /// WORM CAVES
        if (m < 0.0) {
            caveness += max(
                a = (1 - abs(openSimplex96.get(seedA, absX, absY * 2.0, absZ))).pow(5.0 + openSimplex24.get(seedD, absX, absY, absZ)) *
                    (1 - abs(openSimplex64.get(seedB, absX, absY * 1.6, absZ, offset = 272.0))).pow(2.0),
                b = (1 - abs(openSimplex64.get(seedC, absX, absY * 1.4, absZ))).pow(4.0) *
                    (1 - abs(openSimplex48.get(seedD, absX, absY, absZ, offset = 136.0))).pow(1.8)
            ) * if (absY < 18) taper else 1.0
        }

        /// RANDOMIZATION
        val emptinessMini = openSimplex16.get(seedA, absX, absY, absZ, offset = 8.0) / 2.0 + 0.5
        caveness += emptinessMini * .24

        //val totalTime = System.nanoTime() - start
        //caveTime += totalTime
        //caveCount++

        return caveness > threshold
    }

    fun genChunk(chunkX: Int, chunkY: Int, chunkZ: Int): Chunk {
        //val start = System.nanoTime()
        val chunk = Chunk()
        val absChunkX = chunkX * CHUNK_SIZE
        val absChunkZ = chunkZ * CHUNK_SIZE
        val absChunkY = chunkY * CHUNK_SIZE

        for (x in 0 until CHUNK_SIZE) for (z in 0 until CHUNK_SIZE) {

            val absX = (absChunkX + x).toDouble()
            val absZ = (absChunkZ + z).toDouble()

            val (height, flatness) = getHeightAndFlatness(absX, absZ)

            for (y in 0 until CHUNK_SIZE) {
                val absY = (absChunkY + y).toDouble()
                if (genVoxel(absX, absY, absZ, height, flatness)) {
                    chunk[x, y, z] = if (random.nextInt(100) == 0) Block.GLOW_CUBE else Block.STONE
                }
            }

            for (y in 0 until CHUNK_SIZE) {
                val absY = (absChunkY + y).toDouble()
                if (chunk[x, y, z] != null) {
                    if (getCave(absX, absY, absZ, height, flatness)) {
                        chunk[x, y, z] = null
                    } else if (if (y == CHUNK_SIZE - 1) !genVoxel(absX, absY + 1, absZ, height, flatness) else chunk[x, y + 1, z] == null) {
                        chunk[x, y, z] = Block.DIRT
                    }
                }
            }
        }
        //val totalTime = System.nanoTime() - start
        //chunkTime += totalTime
        //chunkCount++
        return chunk
    }

    fun getHeight(x: Int, z: Int): Int = getHeightAndFlatness(x.toDouble(), z.toDouble()).first.toInt()
}