package io.posidon.potassium.tools

import kotlin.math.min

class OpenSimplex2STileableXZ(xzFrequency: Double, repeatX: Int, repeatZ: Int = repeatX) {

    inline fun get(seed: Long, x: Double, y: Double, z: Double, offset: Double = 0.0): Double {
        return noise3(seed, x + offset, y + offset, z + offset)
    }
    inline fun get(seed: Long, x: Double, y: Double, offset: Double = 0.0): Double {
        return noise2(seed, x + offset, y + offset)
    }

    private val m00: Double
    private val m02: Double
    private val m20: Double
    private val m22: Double
    private val m1: Double
    var i00: Float
    var i02: Float
    var i20: Float
    var i22: Float
    var ii00: Long
    var ii02: Long
    var ii20: Long
    var ii22: Long
    var iModX: Long
    var iModZ: Long
    var rSquared: Float
    var approxNormalizer: Float
    private val di: FloatArray
    private val dbp: LongArray

    private fun setLatticePointLookup(index: Int, xrv: Int, yrv: Int, zrv: Int, lattice: Int, nextOnSuccess: Int) {
        var index = index
        index *= 4
        val dxr = lattice * 0.5f - xrv
        val dyr = lattice * 0.5f - yrv
        val dzr = lattice * 0.5f - zrv
        val rxyi = dxr - dyr
        val rzyi = dzr - dyr
        di[index + 0] = i00 * rxyi + i02 * rzyi
        di[index + 1] = I1 * (dxr + dyr + dzr)
        di[index + 2] = i20 * rxyi + i22 * rzyi
        val xyrv = xrv - yrv
        val zyrv = zrv - yrv
        dbp[index + 0] = ii00 * xyrv + ii02 * zyrv
        dbp[index + 1] = (((xrv + yrv + zrv) * 2 + lattice) * PRIME_Y).toLong()
        dbp[index + 2] = ii20 * xyrv + ii22 * zyrv
        dbp[index + 3] = (nextOnSuccess * 4).toLong()
    }

    /*
     * Evaluators
     */
    inline fun noise2(seed: Long, x: Double, z: Double): Double = noise3(seed, x, 0.8660254037844386, z)

    fun noise3(seed: Long, x: Double, y: Double, z: Double): Double {

        // Approximate rotation, adjusted for tiling
        val yy = y * m1
        var rx = m00 * x + m02 * z
        var rz = m20 * x + m22 * z
        val ry = yy - rx - rz
        rx += yy
        rz += yy

        // Relative coordinates like normal
        val rxb = fastFloor(rx)
        val ryb = fastFloor(ry)
        val rzb = fastFloor(rz)
        val rxi = (rx - rxb).toFloat()
        val ryi = (ry - ryb).toFloat()
        val rzi = (rz - rzb).toFloat()

        // Undo entire approximate rotation on relative cell coordinates
        val rxyi = rxi - ryi
        val rzyi = rzi - ryi
        val xi = i00 * rxyi + i02 * rzyi
        val yi = I1 * (rxi + ryi + rzi)
        val zi = i20 * rxyi + i22 * rzyi

        // Undo entire approximate rotation on base vertex coordinates,
        // pre-multiplied by a constant so they stay as integers,
        // and pre-multiplied by primes to speed up hashing,
        // since the modulo can still be performed after prime multiplication.
        val rxyb = rxb - ryb
        val rzyb = rzb - ryb
        val xbp: Long = ii00 * rxyb + ii02 * rzyb
        val ybp: Long = ((rxb + ryb + rzb) * 2 * PRIME_Y).toLong()
        val zbp: Long = ii20 * rxyb + ii22 * rzyb

        // Vertex loop.
        val index = (rxi + 63.5f).toInt() and 64 or ((ryi + 127.5f).toInt() and 128) or ((rzi + 255.5f).toInt() and 256)
        var i = 0
        var value = 0f
        do {
            val dx = xi + di[index or i or 0]
            val dy = yi + di[index or i or 1]
            val dz = zi + di[index or i or 2]
            var a = dx * dx + dy * dy + dz * dz
            if (a < rSquared) {
                // Convert to actual spherical bump function
                a -= rSquared
                a *= a
                a *= a

                // Actual primed modulo-able offsets for this vertex
                var xvp = xbp + dbp[index or i or 0]
                val yvp = ybp + dbp[index or i or 1]
                var zvp = zbp + dbp[index or i or 2]

                // Local modulo. Could be substituted for a true modulo if needed.
                // This is perfectly fine if you only ever evaluate the noise inside (or slightly outside) the repeat boundary.
                if (xvp < 0) xvp += iModX
                else if (xvp >= iModX) xvp -= iModX
                if (zvp < 0) zvp += iModZ
                else if (zvp >= iModZ) zvp -= iModZ

                // Hash
                var hash = seed xor xvp * POSTPRIME_X xor yvp xor zvp * POSTPRIME_Z
                hash *= 2325943009213694033L
                hash = hash xor (hash shr 30)

                // Pseudo-modulo on index to effectively pick from 0-47
                // (repeated like 0012 3345 6678...)
                // This has nothing to do with noise tiling,
                // just clean gradient picking from a non-power-of-two sized set.
                val giL = (hash and 0x3FFFFFFFFFFFFFFL) * 0x555_555_555_555_555L
                val gi = (giL shr 56).toInt() and 0xFC

                // Gradient, multiply, and add.
                value += a * (dx * GRAD3[gi + 0] + dy * GRAD3[gi + 1] + dz * GRAD3[gi + 2])

                // Next on success
                i = dbp[index or i or 3].toInt()
            } else i += 4
        } while (i < 56)
        return (value * approxNormalizer).toDouble()
    }

    companion object {
        // For larger repeat periods combined with higher frequencies, it is possible that pre-multiplying the primes
        // can create overflow in the 64-bit integer coordinate position values, breaking tileability. For more modest
        // use cases, you can enable this for a ~3% performance improvement (according to rudimentary tests on my end).
        // Note that, after moving the pre-prime multiplication to after the trailing zero removal step, I found that
        // cases which break when PREPRIME=true but tile when it's false, became more elusive but were still possible.
        // For example, frequency=0.125 and repeatX=repeatZ=6619035.
        private const val PREPRIME = true
        private const val PRIME_X = 1091
        private const val PRIME_Y = 30869
        private const val PRIME_Z = 1879
        private val PREPRIME_X = if (PREPRIME) PRIME_X else 1
        private val PREPRIME_Z = if (PREPRIME) PRIME_Z else 1
        private val POSTPRIME_X = if (!PREPRIME) PRIME_X else 1
        private val POSTPRIME_Z = if (!PREPRIME) PRIME_Z else 1
        private const val M1 = 0.577350269189626
        private const val I1 = 0.577350269189626f

        private inline fun fastFloor(x: Double): Int {
            val xi = x.toInt()
            return if (x < xi) xi - 1 else xi
        }

        private inline fun fastRoundL(x: Double): Long {
            return if (x > 0) (x + 0.5).toLong() else (x - 0.5).toLong()
        }

        /*
         * Gradients
         */
        private const val N3 = 0.2781926117527186
        var GRAD3: FloatArray

        init {
            val grad3 = doubleArrayOf(
                -2.22474487139, -2.22474487139, -1.0, 0.0,
                -2.22474487139, -2.22474487139, 1.0, 0.0,
                -3.0862664687972017, -1.1721513422464978, 0.0, 0.0,
                -1.1721513422464978, -3.0862664687972017, 0.0, 0.0,
                -2.22474487139, -1.0, -2.22474487139, 0.0,
                -2.22474487139, 1.0, -2.22474487139, 0.0,
                -1.1721513422464978, 0.0, -3.0862664687972017, 0.0,
                -3.0862664687972017, 0.0, -1.1721513422464978, 0.0,
                -2.22474487139, -1.0, 2.22474487139, 0.0,
                -2.22474487139, 1.0, 2.22474487139, 0.0,
                -3.0862664687972017, 0.0, 1.1721513422464978, 0.0,
                -1.1721513422464978, 0.0, 3.0862664687972017, 0.0,
                -2.22474487139, 2.22474487139, -1.0, 0.0,
                -2.22474487139, 2.22474487139, 1.0, 0.0,
                -1.1721513422464978, 3.0862664687972017, 0.0, 0.0,
                -3.0862664687972017, 1.1721513422464978, 0.0, 0.0,
                -1.0, -2.22474487139, -2.22474487139, 0.0,
                1.0, -2.22474487139, -2.22474487139, 0.0,
                0.0, -3.0862664687972017, -1.1721513422464978, 0.0,
                0.0, -1.1721513422464978, -3.0862664687972017, 0.0,
                -1.0, -2.22474487139, 2.22474487139, 0.0,
                1.0, -2.22474487139, 2.22474487139, 0.0,
                0.0, -1.1721513422464978, 3.0862664687972017, 0.0,
                0.0, -3.0862664687972017, 1.1721513422464978, 0.0,
                -1.0, 2.22474487139, -2.22474487139, 0.0,
                1.0, 2.22474487139, -2.22474487139, 0.0,
                0.0, 1.1721513422464978, -3.0862664687972017, 0.0,
                0.0, 3.0862664687972017, -1.1721513422464978, 0.0,
                -1.0, 2.22474487139, 2.22474487139, 0.0,
                1.0, 2.22474487139, 2.22474487139, 0.0,
                0.0, 3.0862664687972017, 1.1721513422464978, 0.0,
                0.0, 1.1721513422464978, 3.0862664687972017, 0.0,
                2.22474487139, -2.22474487139, -1.0, 0.0,
                2.22474487139, -2.22474487139, 1.0, 0.0,
                1.1721513422464978, -3.0862664687972017, 0.0, 0.0,
                3.0862664687972017, -1.1721513422464978, 0.0, 0.0,
                2.22474487139, -1.0, -2.22474487139, 0.0,
                2.22474487139, 1.0, -2.22474487139, 0.0,
                3.0862664687972017, 0.0, -1.1721513422464978, 0.0,
                1.1721513422464978, 0.0, -3.0862664687972017, 0.0,
                2.22474487139, -1.0, 2.22474487139, 0.0,
                2.22474487139, 1.0, 2.22474487139, 0.0,
                1.1721513422464978, 0.0, 3.0862664687972017, 0.0,
                3.0862664687972017, 0.0, 1.1721513422464978, 0.0,
                2.22474487139, 2.22474487139, -1.0, 0.0,
                2.22474487139, 2.22474487139, 1.0, 0.0,
                3.0862664687972017, 1.1721513422464978, 0.0, 0.0,
                1.1721513422464978, 3.0862664687972017, 0.0, 0.0
            )

            // Copy into GRAD, rotated, and with the first of every 3 repeated like 0012 3345 6678.
            GRAD3 = FloatArray(grad3.size * 4 / 3)
            var i = 0
            var j = 0
            while (i < grad3.size) {
                var k = 0
                while (k < 16) {
                    val k2 = if (k == 0) 0 else k - 4
                    val gxr: Double = grad3[i + k2 + 0]
                    val gyr: Double = grad3[i + k2 + 1]
                    val gzr: Double = grad3[i + k2 + 2]
                    val s2: Double = (gxr + gzr) * -0.211324865405187f
                    val yy: Double = gyr * 0.577350269189626f
                    GRAD3[j + k + 0] = ((gxr + s2 - yy) / N3).toFloat()
                    GRAD3[j + k + 1] = ((gxr + gzr + gyr) * 0.577350269189626 / N3).toFloat()
                    GRAD3[j + k + 2] = ((gzr + s2 - yy) / N3).toFloat()
                    k += 4
                }
                i += 4 * 3
                j += 4 * 4
            }
        }
    }

    /*
     * Constructor
     */
    init {

        // Generate initial transform matrix in X and Z, by plugging (repeatX, 0) and (0, repeatZ), multiplied
        // by the frequency, into the 2D / triangular part of the base rotation matrix. Round the results to
        // integers, and divide each vector by repeatX and repeatZ respectively so that when they're used in the
        // adjusted matrix, (repeatX, 0) and (0, repeatZ) produce these integer results which are close to what
        // they would produce ordinarily. One could instead find the true closest points, but this works fine.
        // The resulting matrix includes the frequency.
        val SX0 = repeatX * -0.211324865405187
        val S0Z = repeatZ * -0.211324865405187
        val im00 = fastRoundL(xzFrequency * (repeatX + SX0))
        val im02 = fastRoundL(xzFrequency * S0Z)
        val im20 = fastRoundL(xzFrequency * SX0)
        val im22 = fastRoundL(xzFrequency * (repeatZ + S0Z))
        m00 = im00 / repeatX.toDouble()
        m02 = im02 / repeatZ.toDouble()
        m20 = im20 / repeatX.toDouble()
        m22 = im22 / repeatZ.toDouble()
        m1 = M1 * xzFrequency // yFrequency

        // Integer inverse matrix. When we invert the coordinates of the lattice vertices, which are in the
        // adjusted rotated coordinate space, we want them to stay as integers to keep modulo+hashing accurate.
        // We can achieve this by inverting the general form of the adjusted rotation matrix with the common
        // denominator (matrixScale) multiplied out, as well as multiplying repeatX and repeatZ out of the it
        // from that matrix. Note that, since ii01 = -(ii00 + ii02), ii01 is not stored explicitly. Same for i21.
        // A -1 is shifted to matrixScale, so that the results tend towards positive instead of negative.
        // I must have made an error somewhere else for this to be necessary, but it works now.
        // https://www.wolframalpha.com/input/?i=inverse+%5B%5Ba%2Cq%2Cb%5D%2C%5B-a-c%2Cq%2C-b-d%5D%2C%5Bc%2Cq%2Cd%5D%5D
        ii00 = (im02 + 2L * im22) * repeatX
        ii02 = -(im22 + 2L * im02) * repeatX
        ii20 = -(im00 + 2L * im20) * repeatZ
        ii22 = (im20 + 2L * im00) * repeatZ
        val matrixScale = -3 * (im02 * im20 - im00 * im22)

        // True inverse matrix (with frequency factored out). The above, finally dividing by matrixScale, gives
        // us the inverse of m00..m22. Multiplying by xzFrequency counters the original multiplication by it.
        val inverseMatrixScale = xzFrequency / matrixScale
        i00 = (ii00 * inverseMatrixScale).toFloat()
        i02 = (ii02 * inverseMatrixScale).toFloat()
        i20 = (ii20 * inverseMatrixScale).toFloat()
        i22 = (ii22 * inverseMatrixScale).toFloat()

        // Modulos for the X/Z coordinates returned by that matrix.
        iModX = matrixScale * repeatX
        iModZ = matrixScale * repeatZ

        // Certain repeat periods at certain frequencies create trailing zeroes, which decrease the quality of
        // the hash, and lead to unsightly patterns in the noise. Remove them where we can. An alternative to this
        // could be to always transform back to lattice space after the modulo, but this avoids that runtime cost.
        while (ii00 or ii02 or iModX and 1 == 0L && ii00 or ii02 or iModX != 0L) {
            ii00 = ii00 shr 1
            ii02 = ii02 shr 1
            iModX = iModX shr 1
        }
        while (ii20 or ii22 or iModZ and 1 == 0L && ii20 or ii22 or iModZ != 0L) {
            ii20 = ii20 shr 1
            ii22 = ii22 shr 1
            iModZ = iModZ shr 1
        }

        // Apply hash primes to inverse matrix (if option set). We can modulo with them already in place.
        ii00 *= PREPRIME_X.toLong()
        ii02 *= PREPRIME_X.toLong()
        ii20 *= PREPRIME_Z.toLong()
        ii22 *= PREPRIME_Z.toLong()
        iModX *= PREPRIME_X.toLong()
        iModZ *= PREPRIME_Z.toLong()

        // Avoid discontinuities as the transform slightly changes the distance between lattice vertices.
        val rSquaredA = I1 * I1 * 2.25f
        val rSquaredB = i00 * i00 + i20 * i20 + I1 * I1 * 0.25f
        val rSquaredC = i02 * i02 + i22 * i22 + I1 * I1 * 0.25f
        val rSquaredD = (i00 + i02) * (i00 + i02) + (i20 + i22) * (i20 + i22) + I1 * I1 * 0.25f
        rSquared = min(min(rSquaredA, rSquaredB), min(rSquaredC, rSquaredD))

        // Don't let that push us too far away from a range of -1 to 1 (it will not be exact either way).
        var approxNormalizerDouble = 0.75 / rSquared
        approxNormalizerDouble *= approxNormalizerDouble
        approxNormalizerDouble *= approxNormalizerDouble
        approxNormalizer = approxNormalizerDouble.toFloat()

        // Lookup tables, customized for the repeat period.
        di = FloatArray(8 * 16 * 4)
        dbp = LongArray(8 * 16 * 4)
        for (i in 0..7) {
            val i1: Int = i shr 0 and 1
            val j1: Int = i shr 1 and 1
            val k1: Int = i shr 2 and 1
            val i2: Int = i1 xor 1
            val j2: Int = j1 xor 1
            val k2: Int = k1 xor 1
            var index = i * 16
            setLatticePointLookup(index++, i1, j1, k1, 0, 1)
            setLatticePointLookup(index++, i1 + i2, j1 + j2, k1 + k2, 1, 2)
            setLatticePointLookup(index++, i1 xor 1, j1, k1, 0, 5)
            setLatticePointLookup(index++, i1, j1 xor 1, k1 xor 1, 0, 4)
            setLatticePointLookup(index++, i1 + (i2 xor 1), j1 + j2, k1 + k2, 1, 6)
            setLatticePointLookup(index++, i1 + i2, j1 + (j2 xor 1), k1 + (k2 xor 1), 1, 6)
            setLatticePointLookup(index++, i1, j1 xor 1, k1, 0, 9)
            setLatticePointLookup(index++, i1 xor 1, j1, k1 xor 1, 0, 8)
            setLatticePointLookup(index++, i1 + i2, j1 + (j2 xor 1), k1 + k2, 1, 10)
            setLatticePointLookup(index++, i1 + (i2 xor 1), j1 + j2, k1 + (k2 xor 1), 1, 10)
            setLatticePointLookup(index++, i1, j1, k1 xor 1, 0, 13)
            setLatticePointLookup(index++, i1 xor 1, j1 xor 1, k1, 0, 12)
            setLatticePointLookup(index++, i1 + i2, j1 + j2, k1 + (k2 xor 1), 1, 14)
            setLatticePointLookup(index, i1 + (i2 xor 1), j1 + (j2 xor 1), k1 + k2, 1, 14)
        }
    }
}