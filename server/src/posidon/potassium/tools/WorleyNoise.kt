package posidon.potassium.tools

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The bounds depend on the distance functions,
 * they will be in the interval of: [0.0, 1.0),
 * where the upper max is not fully known,
 * it is recommended to clamp the output.
 */
class WorleyNoise(
    private val seed: Long,
    private val frequency: Double,
    private val mapSize: Int
) {

    inline fun get(x: Double, y: Double, z: Double, offset: Double = 0.0): Double {
        return eval(x + offset, y + offset, z + offset)
    }

    fun eval(x: Double, y: Double): Double {
        val x = x * frequency
        val y = y * frequency
        val iX = floor(x).toLong()
        val iY = floor(y).toLong()
        var shortestDistance = Double.MAX_VALUE
        for (xOffset in -1..1) {
            val secX = iX + xOffset
            val hashX = seed xor X_PRIME * secX
            for (yOffset in -1..1) {
                val secY = iY + yOffset
                val hash = finalizeHash(hashX xor Y_PRIME * secY)
                val rnd = Random(hash)
                val numberFP = fpAmount(rnd)
                for (i in 0 until numberFP) {
                    shortestDistance = min(
                        shortestDistance,
                        distance(
                            x,
                            y,
                            rnd.nextDouble() + secX,
                            rnd.nextDouble() + secY
                        )
                    )
                }
            }
        }
        return sqrt(shortestDistance)
    }

    fun eval(x: Double, y: Double, z: Double): Double {
        val x = x * frequency
        val y = y * frequency
        val z = z * frequency
        val iX = floor(x).toLong()
        val iY = floor(y).toLong()
        val iZ = floor(z).toLong()
        var shortestDistance = Double.MAX_VALUE
        for (xOffset in -1..1) {
            val secX = run {
                val m = mapSize * frequency
                val a = (iX + xOffset) % m
                (if (a < 0) m + a else a).toLong()
            }
            val hashX = seed xor X_PRIME * secX
            for (yOffset in -1..1) {
                val secY = run {
                    val m = mapSize * frequency
                    val a = (iY + yOffset) % m
                    (if (a < 0) m + a else a).toLong()
                }
                val hashY = hashX xor Y_PRIME * secY
                for (zOffset in -1..1) {
                    val secZ = run {
                        val m = mapSize * frequency
                        val a = (iZ + zOffset) % m
                        (if (a < 0) m + a else a).toLong()
                    }
                    val hash = finalizeHash(hashY xor Z_PRIME * secZ)
                    val rnd = Random(hash)
                    val numberFP = fpAmount(rnd)
                    for (i in 0 until numberFP) {
                        shortestDistance = min(
                            shortestDistance,
                            distance(
                                x,
                                y,
                                z,
                                rnd.nextDouble() + secX,
                                rnd.nextDouble() + secY,
                                rnd.nextDouble() + secZ
                            )
                        )
                    }
                }
            }
        }
        return sqrt(shortestDistance)
    }

    fun eval(x: Double, y: Double, z: Double, w: Double): Double {
        val x = x * frequency
        val y = y * frequency
        val z = z * frequency
        val w = w * frequency
        val iX = floor(x).toLong()
        val iY = floor(y).toLong()
        val iZ = floor(z).toLong()
        val iW = floor(w).toLong()
        var shortestDistance = Double.MAX_VALUE
        for (xOffset in -1..1) {
            val secX = iX + xOffset
            val hashX = seed xor X_PRIME * secX
            for (yOffset in -1..1) {
                val secY = iY + yOffset
                val hashY = hashX xor Y_PRIME * secY
                for (zOffset in -1..1) {
                    val secZ = iZ + zOffset
                    val hashZ = hashY xor Z_PRIME * secZ
                    for (wOffset in -1..1) {
                        val secW = iW + wOffset
                        val hash = finalizeHash(hashZ xor W_PRIME * secW)
                        val rnd = Random(hash)
                        val numberFP = fpAmount(rnd)
                        for (i in 0 until numberFP) {
                            shortestDistance = min(
                                shortestDistance,
                                distance(
                                    x,
                                    y,
                                    z,
                                    w,
                                    rnd.nextDouble() + secX,
                                    rnd.nextDouble() + secY,
                                    rnd.nextDouble() + secZ,
                                    rnd.nextDouble() + secW
                                )
                            )
                        }
                    }
                }
            }
        }
        return sqrt(shortestDistance)
    }


    private inline fun fpAmount(rnd: Random) = rnd.nextInt(2, 6)

    private inline fun warp(x: Double) = min(x, mapSize * frequency - x)

    private inline fun distance(x0: Double, y0: Double, x1: Double, y1: Double): Double {
        val x = warp(abs(x0 - x1))
        val y = warp(abs(y0 - y1))
        return x * x + y * y
    }

    private inline fun distance(x0: Double, y0: Double, z0: Double, x1: Double, y1: Double, z1: Double): Double {
        val x = warp(abs(x0 - x1))
        val y = y0 - y1
        val z = warp(abs(z0 - z1))
        return x * x + y * y + z * z
    }

    private inline fun distance(x0: Double, y0: Double, z0: Double, w0: Double, x1: Double, y1: Double, z1: Double, w1: Double): Double {
        val x = x0 - x1
        val y = y0 - y1
        val z = z0 - z1
        val w = w0 - w1
        return x * x + y * y + z * z + w * w
    }

    companion object {
        private const val X_PRIME = 1619
        private const val Y_PRIME = 31337
        private const val Z_PRIME = 6971
        private const val W_PRIME = 1013

        private inline fun finalizeHash(hash: Long): Long {
            val h = hash * hash * hash * 60493
            return h shr 13 xor h
        }
    }
}