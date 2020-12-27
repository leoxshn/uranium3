package posidon.potassium.tools

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.max

class OpenSimplexNoiseTileable3D {

    val scale: Int
    val correction: Double

    inline fun get(x: Double, y: Double, z: Double, offset: Double = 0.0): Double {
        return eval(x / scale * correction + offset, y / scale * correction + offset, z / scale * correction + offset)
    }

    private var perm: ShortArray
    private var permGradIndex3D: ShortArray
    private var w6: Int
    private var h6: Int
    private var d6: Int
    private var sOffset: Int

    constructor(seed: Long, size: Int, height: Int, scale: Int) {
        this.scale = scale
        var seed = seed
        perm = ShortArray(256)
        permGradIndex3D = ShortArray(256)
        val source = ShortArray(256)
        for (i in 0..255) source[i] = i.toShort()
        seed = seed * 6364136223846793005L + 1442695040888963407L
        seed = seed * 6364136223846793005L + 1442695040888963407L
        seed = seed * 6364136223846793005L + 1442695040888963407L
        for (i in 255 downTo 0) {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            var r = ((seed + 31) % (i + 1)).toInt()
            if (r < 0) r += i + 1
            perm[i] = source[r]
            permGradIndex3D[i] = (perm[i] % (gradients3D.size / 3) * 3).toShort()
            source[r] = source[i]
        }
        this.w6 = size / scale / 6
        this.h6 = height / scale / 6
        this.d6 = size / scale / 6

        correction = w6 / (size.toDouble() / scale / 6)

        sOffset = max(w6, max(h6, d6)) * 6
    }

    constructor(perm: ShortArray, w6: Int, h6: Int, d6: Int) {
        this.scale = 1
        this.correction = 1.0
        this.perm = perm
        permGradIndex3D = ShortArray(256)
        this.w6 = w6
        this.h6 = h6
        this.d6 = d6
        sOffset = max(w6, max(h6, d6)) * 6
        for (i in 0..255) {
            permGradIndex3D[i] = (perm[i] % (gradients3D.size / 3) * 3).toShort()
        }
    }

    fun eval(x: Double, y: Double, z: Double): Double {
        val stretchOffset = (x + y + z) * STRETCH_CONSTANT_3D
        val xs = x + stretchOffset
        val ys = y + stretchOffset
        val zs = z + stretchOffset
        var xsb = fastFloor(xs)
        var ysb = fastFloor(ys)
        var zsb = fastFloor(zs)
        val squishOffset = (xsb + ysb + zsb) * SQUISH_CONSTANT_3D
        val xb = xsb + squishOffset
        val yb = ysb + squishOffset
        val zb = zsb + squishOffset
        val xins = xs - xsb
        val yins = ys - ysb
        val zins = zs - zsb
        val inSum = xins + yins + zins
        var dx0 = x - xb
        var dy0 = y - yb
        var dz0 = z - zb
        xsb += sOffset
        ysb += sOffset
        zsb += sOffset
        val dx_ext0: Double
        var dy_ext0: Double
        val dz_ext0: Double
        var dx_ext1: Double
        var dy_ext1: Double
        var dz_ext1: Double
        val xsv_ext0: Int
        var ysv_ext0: Int
        val zsv_ext0: Int
        var xsv_ext1: Int
        var ysv_ext1: Int
        var zsv_ext1: Int
        var value = 0.0
        if (inSum <= 1) {
            var aPoint: Byte = 0x01
            var aScore = xins
            var bPoint: Byte = 0x02
            var bScore = yins
            if (aScore >= bScore && zins > bScore) {
                bScore = zins
                bPoint = 0x04
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins
                aPoint = 0x04
            }
            val wins = 1 - inSum
            if (wins > aScore || wins > bScore) {
                val c = if (bScore > aScore) bPoint else aPoint
                if (c and 0x01 == 0.toByte()) {
                    xsv_ext0 = xsb - 1
                    xsv_ext1 = xsb
                    dx_ext0 = dx0 + 1
                    dx_ext1 = dx0
                } else {
                    xsv_ext1 = xsb + 1
                    xsv_ext0 = xsv_ext1
                    dx_ext1 = dx0 - 1
                    dx_ext0 = dx_ext1
                }
                if (c and 0x02 == 0.toByte()) {
                    ysv_ext1 = ysb
                    ysv_ext0 = ysv_ext1
                    dy_ext1 = dy0
                    dy_ext0 = dy_ext1
                    if (c and 0x01 == 0.toByte()) {
                        ysv_ext1 -= 1
                        dy_ext1 += 1.0
                    } else {
                        ysv_ext0 -= 1
                        dy_ext0 += 1.0
                    }
                } else {
                    ysv_ext1 = ysb + 1
                    ysv_ext0 = ysv_ext1
                    dy_ext1 = dy0 - 1
                    dy_ext0 = dy_ext1
                }
                if (c and 0x04 == 0.toByte()) {
                    zsv_ext0 = zsb
                    zsv_ext1 = zsb - 1
                    dz_ext0 = dz0
                    dz_ext1 = dz0 + 1
                } else {
                    zsv_ext1 = zsb + 1
                    zsv_ext0 = zsv_ext1
                    dz_ext1 = dz0 - 1
                    dz_ext0 = dz_ext1
                }
            } else {
                val c = (aPoint or bPoint)
                if (c and 0x01 == 0.toByte()) {
                    xsv_ext0 = xsb
                    xsv_ext1 = xsb - 1
                    dx_ext0 = dx0 - 2 * SQUISH_CONSTANT_3D
                    dx_ext1 = dx0 + 1 - SQUISH_CONSTANT_3D
                } else {
                    xsv_ext1 = xsb + 1
                    xsv_ext0 = xsv_ext1
                    dx_ext0 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D
                    dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D
                }
                if (c and 0x02 == 0.toByte()) {
                    ysv_ext0 = ysb
                    ysv_ext1 = ysb - 1
                    dy_ext0 = dy0 - 2 * SQUISH_CONSTANT_3D
                    dy_ext1 = dy0 + 1 - SQUISH_CONSTANT_3D
                } else {
                    ysv_ext1 = ysb + 1
                    ysv_ext0 = ysv_ext1
                    dy_ext0 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D
                    dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D
                }
                if (c and 0x04 == 0.toByte()) {
                    zsv_ext0 = zsb
                    zsv_ext1 = zsb - 1
                    dz_ext0 = dz0 - 2 * SQUISH_CONSTANT_3D
                    dz_ext1 = dz0 + 1 - SQUISH_CONSTANT_3D
                } else {
                    zsv_ext1 = zsb + 1
                    zsv_ext0 = zsv_ext1
                    dz_ext0 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D
                    dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D
                }
            }
            var attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0
            if (attn0 > 0) {
                attn0 *= attn0
                value += attn0 * attn0 * extrapolate(xsb + 0, ysb + 0, zsb + 0, dx0, dy0, dz0)
            }
            val dx1 = dx0 - 1 - SQUISH_CONSTANT_3D
            val dy1 = dy0 - 0 - SQUISH_CONSTANT_3D
            val dz1 = dz0 - 0 - SQUISH_CONSTANT_3D
            var attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1
            if (attn1 > 0) {
                attn1 *= attn1
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1)
            }
            val dx2 = dx0 - 0 - SQUISH_CONSTANT_3D
            val dy2 = dy0 - 1 - SQUISH_CONSTANT_3D
            var attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1
            if (attn2 > 0) {
                attn2 *= attn2
                value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz1)
            }
            val dz3 = dz0 - 1 - SQUISH_CONSTANT_3D
            var attn3 = 2 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3
            if (attn3 > 0) {
                attn3 *= attn3
                value += attn3 * attn3 * extrapolate(xsb + 0, ysb + 0, zsb + 1, dx2, dy1, dz3)
            }
        } else if (inSum >= 2) {
            var aPoint: Byte = 0x06
            var aScore = xins
            var bPoint: Byte = 0x05
            var bScore = yins
            if (aScore <= bScore && zins < bScore) {
                bScore = zins
                bPoint = 0x03
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins
                aPoint = 0x03
            }
            val wins = 3 - inSum
            if (wins < aScore || wins < bScore) {
                val c = if (bScore < aScore) bPoint else aPoint
                if (c and 0x01 != 0.toByte()) {
                    xsv_ext0 = xsb + 2
                    xsv_ext1 = xsb + 1
                    dx_ext0 = dx0 - 2 - 3 * SQUISH_CONSTANT_3D
                    dx_ext1 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D
                } else {
                    xsv_ext1 = xsb
                    xsv_ext0 = xsv_ext1
                    dx_ext1 = dx0 - 3 * SQUISH_CONSTANT_3D
                    dx_ext0 = dx_ext1
                }
                if (c and 0x02 != 0.toByte()) {
                    ysv_ext1 = ysb + 1
                    ysv_ext0 = ysv_ext1
                    dy_ext1 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D
                    dy_ext0 = dy_ext1
                    if (c and 0x01 != 0.toByte()) {
                        ysv_ext1 += 1
                        dy_ext1 -= 1.0
                    } else {
                        ysv_ext0 += 1
                        dy_ext0 -= 1.0
                    }
                } else {
                    ysv_ext1 = ysb
                    ysv_ext0 = ysv_ext1
                    dy_ext1 = dy0 - 3 * SQUISH_CONSTANT_3D
                    dy_ext0 = dy_ext1
                }
                if (c and 0x04 != 0.toByte()) {
                    zsv_ext0 = zsb + 1
                    zsv_ext1 = zsb + 2
                    dz_ext0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D
                    dz_ext1 = dz0 - 2 - 3 * SQUISH_CONSTANT_3D
                } else {
                    zsv_ext1 = zsb
                    zsv_ext0 = zsv_ext1
                    dz_ext1 = dz0 - 3 * SQUISH_CONSTANT_3D
                    dz_ext0 = dz_ext1
                }
            } else {
                val c = (aPoint and bPoint)
                if (c and 0x01 != 0.toByte()) {
                    xsv_ext0 = xsb + 1
                    xsv_ext1 = xsb + 2
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D
                    dx_ext1 = dx0 - 2 - 2 * SQUISH_CONSTANT_3D
                } else {
                    xsv_ext1 = xsb
                    xsv_ext0 = xsv_ext1
                    dx_ext0 = dx0 - SQUISH_CONSTANT_3D
                    dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D
                }
                if (c and 0x02 != 0.toByte()) {
                    ysv_ext0 = ysb + 1
                    ysv_ext1 = ysb + 2
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D
                    dy_ext1 = dy0 - 2 - 2 * SQUISH_CONSTANT_3D
                } else {
                    ysv_ext1 = ysb
                    ysv_ext0 = ysv_ext1
                    dy_ext0 = dy0 - SQUISH_CONSTANT_3D
                    dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D
                }
                if (c and 0x04 != 0.toByte()) {
                    zsv_ext0 = zsb + 1
                    zsv_ext1 = zsb + 2
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D
                    dz_ext1 = dz0 - 2 - 2 * SQUISH_CONSTANT_3D
                } else {
                    zsv_ext1 = zsb
                    zsv_ext0 = zsv_ext1
                    dz_ext0 = dz0 - SQUISH_CONSTANT_3D
                    dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D
                }
            }
            val dx3 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D
            val dy3 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D
            val dz3 = dz0 - 0 - 2 * SQUISH_CONSTANT_3D
            var attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3
            if (attn3 > 0) {
                attn3 *= attn3
                value += attn3 * attn3 * extrapolate(xsb + 1, ysb + 1, zsb + 0, dx3, dy3, dz3)
            }
            val dy2 = dy0 - 0 - 2 * SQUISH_CONSTANT_3D
            val dz2 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D
            var attn2 = 2 - dx3 * dx3 - dy2 * dy2 - dz2 * dz2
            if (attn2 > 0) {
                attn2 *= attn2
                value += attn2 * attn2 * extrapolate(xsb + 1, ysb + 0, zsb + 1, dx3, dy2, dz2)
            }
            val dx1 = dx0 - 0 - 2 * SQUISH_CONSTANT_3D
            var attn1 = 2 - dx1 * dx1 - dy3 * dy3 - dz2 * dz2
            if (attn1 > 0) {
                attn1 *= attn1
                value += attn1 * attn1 * extrapolate(xsb + 0, ysb + 1, zsb + 1, dx1, dy3, dz2)
            }
            dx0 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D
            dy0 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D
            dz0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D
            var attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0
            if (attn0 > 0) {
                attn0 *= attn0
                value += attn0 * attn0 * extrapolate(xsb + 1, ysb + 1, zsb + 1, dx0, dy0, dz0)
            }
        } else {
            var aScore: Double
            var aPoint: Byte
            var aIsFurtherSide: Boolean
            var bScore: Double
            var bPoint: Byte
            var bIsFurtherSide: Boolean
            val p1 = xins + yins
            if (p1 > 1) {
                aScore = p1 - 1
                aPoint = 0x03
                aIsFurtherSide = true
            } else {
                aScore = 1 - p1
                aPoint = 0x04
                aIsFurtherSide = false
            }
            val p2 = xins + zins
            if (p2 > 1) {
                bScore = p2 - 1
                bPoint = 0x05
                bIsFurtherSide = true
            } else {
                bScore = 1 - p2
                bPoint = 0x02
                bIsFurtherSide = false
            }
            val p3 = yins + zins
            if (p3 > 1) {
                val score = p3 - 1
                if (aScore <= bScore && aScore < score) {
                    aScore = score
                    aPoint = 0x06
                    aIsFurtherSide = true
                } else if (aScore > bScore && bScore < score) {
                    bScore = score
                    bPoint = 0x06
                    bIsFurtherSide = true
                }
            } else {
                val score = 1 - p3
                if (aScore <= bScore && aScore < score) {
                    aScore = score
                    aPoint = 0x01
                    aIsFurtherSide = false
                } else if (aScore > bScore && bScore < score) {
                    bScore = score
                    bPoint = 0x01
                    bIsFurtherSide = false
                }
            }
            if (aIsFurtherSide == bIsFurtherSide) {
                if (aIsFurtherSide) {
                    dx_ext0 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D
                    dy_ext0 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D
                    dz_ext0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D
                    xsv_ext0 = xsb + 1
                    ysv_ext0 = ysb + 1
                    zsv_ext0 = zsb + 1
                    val c = (aPoint and bPoint)
                    if (c and 0x01 != 0.toByte()) {
                        dx_ext1 = dx0 - 2 - 2 * SQUISH_CONSTANT_3D
                        dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D
                        dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D
                        xsv_ext1 = xsb + 2
                        ysv_ext1 = ysb
                        zsv_ext1 = zsb
                    } else if (c and 0x02 != 0.toByte()) {
                        dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D
                        dy_ext1 = dy0 - 2 - 2 * SQUISH_CONSTANT_3D
                        dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D
                        xsv_ext1 = xsb
                        ysv_ext1 = ysb + 2
                        zsv_ext1 = zsb
                    } else {
                        dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D
                        dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D
                        dz_ext1 = dz0 - 2 - 2 * SQUISH_CONSTANT_3D
                        xsv_ext1 = xsb
                        ysv_ext1 = ysb
                        zsv_ext1 = zsb + 2
                    }
                } else {
                    dx_ext0 = dx0
                    dy_ext0 = dy0
                    dz_ext0 = dz0
                    xsv_ext0 = xsb
                    ysv_ext0 = ysb
                    zsv_ext0 = zsb
                    val c = (aPoint or bPoint)
                    if (c and 0x01 == 0.toByte()) {
                        dx_ext1 = dx0 + 1 - SQUISH_CONSTANT_3D
                        dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D
                        dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D
                        xsv_ext1 = xsb - 1
                        ysv_ext1 = ysb + 1
                        zsv_ext1 = zsb + 1
                    } else if (c and 0x02 == 0.toByte()) {
                        dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D
                        dy_ext1 = dy0 + 1 - SQUISH_CONSTANT_3D
                        dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D
                        xsv_ext1 = xsb + 1
                        ysv_ext1 = ysb - 1
                        zsv_ext1 = zsb + 1
                    } else {
                        dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D
                        dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D
                        dz_ext1 = dz0 + 1 - SQUISH_CONSTANT_3D
                        xsv_ext1 = xsb + 1
                        ysv_ext1 = ysb + 1
                        zsv_ext1 = zsb - 1
                    }
                }
            } else {
                val c1: Byte
                val c2: Byte
                if (aIsFurtherSide) {
                    c1 = aPoint
                    c2 = bPoint
                } else {
                    c1 = bPoint
                    c2 = aPoint
                }
                if (c1 and 0x01 == 0.toByte()) {
                    dx_ext0 = dx0 + 1 - SQUISH_CONSTANT_3D
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D
                    xsv_ext0 = xsb - 1
                    ysv_ext0 = ysb + 1
                    zsv_ext0 = zsb + 1
                } else if (c1 and 0x02 == 0.toByte()) {
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D
                    dy_ext0 = dy0 + 1 - SQUISH_CONSTANT_3D
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D
                    xsv_ext0 = xsb + 1
                    ysv_ext0 = ysb - 1
                    zsv_ext0 = zsb + 1
                } else {
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D
                    dz_ext0 = dz0 + 1 - SQUISH_CONSTANT_3D
                    xsv_ext0 = xsb + 1
                    ysv_ext0 = ysb + 1
                    zsv_ext0 = zsb - 1
                }
                dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D
                dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D
                dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D
                xsv_ext1 = xsb
                ysv_ext1 = ysb
                zsv_ext1 = zsb
                if (c2 and 0x01 != 0.toByte()) {
                    dx_ext1 -= 2.0
                    xsv_ext1 += 2
                } else if (c2 and 0x02 != 0.toByte()) {
                    dy_ext1 -= 2.0
                    ysv_ext1 += 2
                } else {
                    dz_ext1 -= 2.0
                    zsv_ext1 += 2
                }
            }
            val dx1 = dx0 - 1 - SQUISH_CONSTANT_3D
            val dy1 = dy0 - 0 - SQUISH_CONSTANT_3D
            val dz1 = dz0 - 0 - SQUISH_CONSTANT_3D
            var attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1
            if (attn1 > 0) {
                attn1 *= attn1
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1)
            }
            val dx2 = dx0 - 0 - SQUISH_CONSTANT_3D
            val dy2 = dy0 - 1 - SQUISH_CONSTANT_3D
            var attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1
            if (attn2 > 0) {
                attn2 *= attn2
                value += attn2 * attn2 * extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz1)
            }
            val dz3 = dz0 - 1 - SQUISH_CONSTANT_3D
            var attn3 = 2 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3
            if (attn3 > 0) {
                attn3 *= attn3
                value += attn3 * attn3 * extrapolate(xsb + 0, ysb + 0, zsb + 1, dx2, dy1, dz3)
            }
            val dx4 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D
            val dy4 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D
            val dz4 = dz0 - 0 - 2 * SQUISH_CONSTANT_3D
            var attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4
            if (attn4 > 0) {
                attn4 *= attn4
                value += attn4 * attn4 * extrapolate(xsb + 1, ysb + 1, zsb + 0, dx4, dy4, dz4)
            }
            val dy5 = dy0 - 0 - 2 * SQUISH_CONSTANT_3D
            val dz5 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D
            var attn5 = 2 - dx4 * dx4 - dy5 * dy5 - dz5 * dz5
            if (attn5 > 0) {
                attn5 *= attn5
                value += attn5 * attn5 * extrapolate(xsb + 1, ysb + 0, zsb + 1, dx4, dy5, dz5)
            }
            val dx6 = dx0 - 0 - 2 * SQUISH_CONSTANT_3D
            var attn6 = 2 - dx6 * dx6 - dy4 * dy4 - dz5 * dz5
            if (attn6 > 0) {
                attn6 *= attn6
                value += attn6 * attn6 * extrapolate(xsb + 0, ysb + 1, zsb + 1, dx6, dy4, dz5)
            }
        }
        var attn_ext0 = 2 - dx_ext0 * dx_ext0 - dy_ext0 * dy_ext0 - dz_ext0 * dz_ext0
        if (attn_ext0 > 0) {
            attn_ext0 *= attn_ext0
            value += attn_ext0 * attn_ext0 * extrapolate(xsv_ext0, ysv_ext0, zsv_ext0, dx_ext0, dy_ext0, dz_ext0)
        }
        var attn_ext1 = 2 - dx_ext1 * dx_ext1 - dy_ext1 * dy_ext1 - dz_ext1 * dz_ext1
        if (attn_ext1 > 0) {
            attn_ext1 *= attn_ext1
            value += attn_ext1 * attn_ext1 * extrapolate(xsv_ext1, ysv_ext1, zsv_ext1, dx_ext1, dy_ext1, dz_ext1)
        }
        return value / NORM_CONSTANT_3D
    }

    private inline fun extrapolate(xsb: Int, ysb: Int, zsb: Int, dx: Double, dy: Double, dz: Double): Double {
        val bSum = xsb + ysb + zsb
        val xc = (3 * xsb + bSum) / 18 / w6
        val yc = (3 * ysb + bSum) / 18 / h6
        val zc = (3 * zsb + bSum) / 18 / d6
        val xsbm = -5 * w6 * xc + h6 * yc + d6 * zc + xsb
        val ysbm = w6 * xc + -5 * h6 * yc + d6 * zc + ysb
        val zsbm = w6 * xc + h6 * yc + -5 * d6 * zc + zsb
        val index = permGradIndex3D[perm[perm[xsbm and 0xFF] + ysbm and 0xFF] + zsbm and 0xFF].toInt()
        return gradients3D[index] * dx + gradients3D[index + 1] * dy + gradients3D[index + 2] * dz
    }

    companion object {
        private const val STRETCH_CONSTANT_3D = -1.0 / 6.0 //(1/sqrt(3+1)-1)/3
        private const val SQUISH_CONSTANT_3D = 1.0 / 3.0 //(sqrt(3+1)-1)/3
        private const val NORM_CONSTANT_3D = 103.0

        private inline fun fastFloor(x: Double): Int {
            val xi = x.toInt()
            return if (x < xi) xi - 1 else xi
        }

        private val gradients3D = byteArrayOf(
                -11, 4, 4, -4, 11, 4, -4, 4, 11,
                11, 4, 4, 4, 11, 4, 4, 4, 11,
                -11, -4, 4, -4, -11, 4, -4, -4, 11,
                11, -4, 4, 4, -11, 4, 4, -4, 11,
                -11, 4, -4, -4, 11, -4, -4, 4, -11,
                11, 4, -4, 4, 11, -4, 4, 4, -11,
                -11, -4, -4, -4, -11, -4, -4, -4, -11,
                11, -4, -4, 4, -11, -4, 4, -4, -11)
    }
}