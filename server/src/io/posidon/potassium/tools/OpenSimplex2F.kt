package io.posidon.potassium.tools

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OpenSimplex2F(seed: Long, val mapSize: Int) {


    fun tile(x: Double, y: Double, scale: Int = 1, offset: Double = 0.0): Double {

        val tau = 2 * PI

        val s = x / mapSize
        val t = y / mapSize

        val nx = (offset + cos(tau * s) * -2 / tau) / scale * mapSize
        val ny = (offset + cos(tau * t) * -2 / tau) / scale * mapSize
        val nz = (offset + sin(tau * s) * -2 / tau) / scale * mapSize
        val nw = (offset + sin(tau * t) * -2 / tau) / scale * mapSize

        return noise4_XZBeforeYW(nx, ny, nz, nw)
    }

    fun tile(x: Double, y: Double, z: Double, scale: Int = 1, offset: Double = 0.0): Double {

        val tau = 2 * PI

        val s = x / mapSize
        val t = z / mapSize

        val nx = (offset + cos(tau * s) * -2 / tau) / scale * mapSize
        val ny = (offset + cos(tau * t) * -2 / tau + y) / scale * mapSize
        val nz = (offset + sin(tau * s) * -2 / tau - y) / scale * mapSize
        val nw = (offset + sin(tau * t) * -2 / tau) / scale * mapSize

        return noise4_XZBeforeYW(nx, ny, nz, nw)
    }


    private val perm: ShortArray
    private val permGrad2: Array<Grad2?>
    private val permGrad3: Array<Grad3?>
    private val permGrad4: Array<Grad4?>

    /**
     * 2D Simplex noise, standard lattice orientation.
     */
    fun noise2(x: Double, y: Double): Double {

        // Get points for A2* lattice
        val s = 0.366025403784439 * (x + y)
        val xs = x + s
        val ys = y + s
        return noise2_Base(xs, ys)
    }

    /**
     * 2D Simplex noise, with Y pointing down the main diagonal.
     * Might be better for a 2D sandbox style game, where Y is vertical.
     * Probably slightly less optimal for heightmaps or continent maps.
     */
    fun noise2_XBeforeY(x: Double, y: Double): Double {

        // Skew transform and rotation baked into one.
        val xx = x * 0.7071067811865476
        val yy = y * 1.224744871380249
        return noise2_Base(yy + xx, yy - xx)
    }

    /**
     * 2D Simplex noise base.
     * Lookup table implementation inspired by DigitalShadow.
     */
    private fun noise2_Base(xs: Double, ys: Double): Double {
        var value = 0.0

        // Get base points and offsets
        val xsb = fastFloor(xs)
        val ysb = fastFloor(ys)
        val xsi = xs - xsb
        val ysi = ys - ysb

        // Index to point list
        val index = ((ysi - xsi) / 2 + 1).toInt()
        val ssi = (xsi + ysi) * -0.211324865405187
        val xi = xsi + ssi
        val yi = ysi + ssi

        // Point contributions
        for (i in 0..2) {
            val c = LOOKUP_2D[index + i]
            val dx = xi + c!!.dx
            val dy = yi + c.dy
            var attn = 0.5 - dx * dx - dy * dy
            if (attn <= 0) continue
            val pxm = xsb + c.xsv and PMASK
            val pym = ysb + c.ysv and PMASK
            val grad = permGrad2[perm[pxm].toInt() xor pym]
            val extrapolation = grad!!.dx * dx + grad.dy * dy
            attn *= attn
            value += attn * attn * extrapolation
        }
        return value
    }

    /**
     * 3D Re-oriented 4-point BCC noise, classic orientation.
     * Proper substitute for 3D Simplex in light of Forbidden Formulae.
     * Use noise3_XYBeforeZ or noise3_XZBeforeY instead, wherever appropriate.
     */
    fun noise3_Classic(x: Double, y: Double, z: Double): Double {

        // Re-orient the cubic lattices via rotation, to produce the expected look on cardinal planar slices.
        // If texturing objects that don't tend to have cardinal plane faces, you could even remove this.
        // Orthonormal rotation. Not a skew transform.
        val r = 2.0 / 3.0 * (x + y + z)
        val xr = r - x
        val yr = r - y
        val zr = r - z

        // Evaluate both lattices to form a BCC lattice.
        return noise3_BCC(xr, yr, zr)
    }

    /**
     * 3D Re-oriented 4-point BCC noise, with better visual isotropy in (X, Y).
     * Recommended for 3D terrain and time-varied animations.
     * The Z coordinate should always be the "different" coordinate in your use case.
     * If Y is vertical in world coordinates, call noise3_XYBeforeZ(x, z, Y) or use noise3_XZBeforeY.
     * If Z is vertical in world coordinates, call noise3_XYBeforeZ(x, y, Z).
     * For a time varied animation, call noise3_XYBeforeZ(x, y, T).
     */
    fun noise3_XYBeforeZ(x: Double, y: Double, z: Double): Double {

        // Re-orient the cubic lattices without skewing, to make X and Y triangular like 2D.
        // Orthonormal rotation. Not a skew transform.
        val xy = x + y
        val s2 = xy * -0.211324865405187
        val zz = z * 0.577350269189626
        val xr = x + s2 - zz
        val yr = y + s2 - zz
        val zr = xy * 0.577350269189626 + zz

        // Evaluate both lattices to form a BCC lattice.
        return noise3_BCC(xr, yr, zr)
    }

    /**
     * 3D Re-oriented 4-point BCC noise, with better visual isotropy in (X, Z).
     * Recommended for 3D terrain and time-varied animations.
     * The Y coordinate should always be the "different" coordinate in your use case.
     * If Y is vertical in world coordinates, call noise3_XZBeforeY(x, Y, z).
     * If Z is vertical in world coordinates, call noise3_XZBeforeY(x, Z, y) or use noise3_XYBeforeZ.
     * For a time varied animation, call noise3_XZBeforeY(x, T, y) or use noise3_XYBeforeZ.
     */
    fun noise3_XZBeforeY(x: Double, y: Double, z: Double): Double {

        // Re-orient the cubic lattices without skewing, to make X and Z triangular like 2D.
        // Orthonormal rotation. Not a skew transform.
        val xz = x + z
        val s2 = xz * -0.211324865405187
        val yy = y * 0.577350269189626
        val xr = x + s2 - yy
        val zr = z + s2 - yy
        val yr = xz * 0.577350269189626 + yy

        // Evaluate both lattices to form a BCC lattice.
        return noise3_BCC(xr, yr, zr)
    }

    /**
     * Generate overlapping cubic lattices for 3D Re-oriented BCC noise.
     * Lookup table implementation inspired by DigitalShadow.
     * It was actually faster to narrow down the points in the loop itself,
     * than to build up the index with enough info to isolate 4 points.
     */
    private fun noise3_BCC(xr: Double, yr: Double, zr: Double): Double {

        // Get base and offsets inside cube of first lattice.
        val xrb = fastFloor(xr)
        val yrb = fastFloor(yr)
        val zrb = fastFloor(zr)
        val xri = xr - xrb
        val yri = yr - yrb
        val zri = zr - zrb

        // Identify which octant of the cube we're in. This determines which cell
        // in the other cubic lattice we're in, and also narrows down one point on each.
        val xht = (xri + 0.5).toInt()
        val yht = (yri + 0.5).toInt()
        val zht = (zri + 0.5).toInt()
        val index = xht shl 0 or (yht shl 1) or (zht shl 2)

        // Point contributions
        var value = 0.0
        var c = LOOKUP_3D[index]
        while (c != null) {
            val dxr = xri + c.dxr
            val dyr = yri + c.dyr
            val dzr = zri + c.dzr
            var attn = 0.5 - dxr * dxr - dyr * dyr - dzr * dzr
            if (attn < 0) {
                c = c.nextOnFailure
            } else {
                val pxm = xrb + c.xrv and PMASK
                val pym = yrb + c.yrv and PMASK
                val pzm = zrb + c.zrv and PMASK
                val grad = permGrad3[perm[perm[pxm].toInt() xor pym].toInt() xor pzm]
                val extrapolation = grad!!.dx * dxr + grad.dy * dyr + grad.dz * dzr
                attn *= attn
                value += attn * attn * extrapolation
                c = c.nextOnSuccess
            }
        }
        return value
    }

    /**
     * 4D OpenSimplex2F noise, classic lattice orientation.
     */
    fun noise4_Classic(x: Double, y: Double, z: Double, w: Double): Double {

        // Get points for A4 lattice
        val s = -0.138196601125011 * (x + y + z + w)
        val xs = x + s
        val ys = y + s
        val zs = z + s
        val ws = w + s
        return noise4_Base(xs, ys, zs, ws)
    }

    /**
     * 4D OpenSimplex2F noise, with XY and ZW forming orthogonal triangular-based planes.
     * Recommended for 3D terrain, where X and Y (or Z and W) are horizontal.
     * Recommended for noise(x, y, sin(time), cos(time)) trick.
     */
    fun noise4_XYBeforeZW(x: Double, y: Double, z: Double, w: Double): Double {
        val s2 = (x + y) * -0.178275657951399372 + (z + w) * 0.215623393288842828
        val t2 = (z + w) * -0.403949762580207112 + (x + y) * -0.375199083010075342
        val xs = x + s2
        val ys = y + s2
        val zs = z + t2
        val ws = w + t2
        return noise4_Base(xs, ys, zs, ws)
    }

    /**
     * 4D OpenSimplex2F noise, with XZ and YW forming orthogonal triangular-based planes.
     * Recommended for 3D terrain, where X and Z (or Y and W) are horizontal.
     */
    fun noise4_XZBeforeYW(x: Double, y: Double, z: Double, w: Double): Double {
        val s2 = (x + z) * -0.178275657951399372 + (y + w) * 0.215623393288842828
        val t2 = (y + w) * -0.403949762580207112 + (x + z) * -0.375199083010075342
        val xs = x + s2
        val ys = y + t2
        val zs = z + s2
        val ws = w + t2
        return noise4_Base(xs, ys, zs, ws)
    }

    /**
     * 4D OpenSimplex2F noise, with XYZ oriented like noise3_Classic,
     * and W for an extra degree of freedom. W repeats eventually.
     * Recommended for time-varied animations which texture a 3D object (W=time)
     */
    fun noise4_XYZBeforeW(x: Double, y: Double, z: Double, w: Double): Double {
        val xyz = x + y + z
        val ww = w * 0.2236067977499788
        val s2 = xyz * -0.16666666666666666 + ww
        val xs = x + s2
        val ys = y + s2
        val zs = z + s2
        val ws = -0.5 * xyz + ww
        return noise4_Base(xs, ys, zs, ws)
    }

    /**
     * 4D OpenSimplex2F noise base.
     * Current implementation not fully optimized by lookup tables.
     * But still comes out slightly ahead of Gustavson's Simplex in tests.
     */
    private fun noise4_Base(xs: Double, ys: Double, zs: Double, ws: Double): Double {
        var value = 0.0

        // Get base points and offsets
        var xsb = fastFloor(xs)
        var ysb = fastFloor(ys)
        var zsb = fastFloor(zs)
        var wsb = fastFloor(ws)
        var xsi = xs - xsb
        var ysi = ys - ysb
        var zsi = zs - zsb
        var wsi = ws - wsb

        // If we're in the lower half, flip so we can repeat the code for the upper half. We'll flip back later.
        var siSum = xsi + ysi + zsi + wsi
        var ssi = siSum * 0.309016994374947 // Prep for vertex contributions.
        val inLowerHalf = siSum < 2
        if (inLowerHalf) {
            xsi = 1 - xsi
            ysi = 1 - ysi
            zsi = 1 - zsi
            wsi = 1 - wsi
            siSum = 4 - siSum
        }

        // Consider opposing vertex pairs of the octahedron formed by the central cross-section of the stretched tesseract
        val aabb = xsi + ysi - zsi - wsi
        val abab = xsi - ysi + zsi - wsi
        val abba = xsi - ysi - zsi + wsi
        val aabbScore = Math.abs(aabb)
        val ababScore = Math.abs(abab)
        val abbaScore = Math.abs(abba)

        // Find the closest point on the stretched tesseract as if it were the upper half
        var vertexIndex: Int
        var via: Int
        val vib: Int
        var asi: Double
        var bsi: Double
        if (aabbScore > ababScore && aabbScore > abbaScore) {
            if (aabb > 0) {
                asi = zsi
                bsi = wsi
                vertexIndex = 3
                via = 7
                vib = 11
            } else {
                asi = xsi
                bsi = ysi
                vertexIndex = 12
                via = 13
                vib = 14
            }
        } else if (ababScore > abbaScore) {
            if (abab > 0) {
                asi = ysi
                bsi = wsi
                vertexIndex = 5
                via = 7
                vib = 13
            } else {
                asi = xsi
                bsi = zsi
                vertexIndex = 10
                via = 11
                vib = 14
            }
        } else {
            if (abba > 0) {
                asi = ysi
                bsi = zsi
                vertexIndex = 9
                via = 11
                vib = 13
            } else {
                asi = xsi
                bsi = wsi
                vertexIndex = 6
                via = 7
                vib = 14
            }
        }
        if (bsi > asi) {
            via = vib
            val temp = bsi
            bsi = asi
            asi = temp
        }
        if (siSum + asi > 3) {
            vertexIndex = via
            if (siSum + bsi > 4) {
                vertexIndex = 15
            }
        }

        // Now flip back if we're actually in the lower half.
        if (inLowerHalf) {
            xsi = 1 - xsi
            ysi = 1 - ysi
            zsi = 1 - zsi
            wsi = 1 - wsi
            vertexIndex = vertexIndex xor 15
        }

        // Five points to add, total, from five copies of the A4 lattice.
        for (i in 0..4) {

            // Update xsb/etc. and add the lattice point's contribution.
            val c = VERTICES_4D[vertexIndex]
            xsb += c!!.xsv
            ysb += c.ysv
            zsb += c.zsv
            wsb += c.wsv
            val xi = xsi + ssi
            val yi = ysi + ssi
            val zi = zsi + ssi
            val wi = wsi + ssi
            val dx = xi + c.dx
            val dy = yi + c.dy
            val dz = zi + c.dz
            val dw = wi + c.dw
            var attn = 0.5 - dx * dx - dy * dy - dz * dz - dw * dw
            if (attn > 0) {
                val pxm = xsb and PMASK
                val pym = ysb and PMASK
                val pzm = zsb and PMASK
                val pwm = wsb and PMASK
                val grad = permGrad4[perm[perm[perm[pxm].toInt() xor pym].toInt() xor pzm].toInt() xor pwm]
                val ramped = grad!!.dx * dx + grad.dy * dy + grad.dz * dz + grad.dw * dw
                attn *= attn
                value += attn * attn * ramped
            }

            // Maybe this helps the compiler/JVM/LLVM/etc. know we can end the loop here. Maybe not.
            if (i == 4) break

            // Update the relative skewed coordinates to reference the vertex we just added.
            // Rather, reference its counterpart on the lattice copy that is shifted down by
            // the vector <-0.2, -0.2, -0.2, -0.2>
            xsi += c.xsi
            ysi += c.ysi
            zsi += c.zsi
            wsi += c.wsi
            ssi += c.ssiDelta

            // Next point is the closest vertex on the 4-simplex whose base vertex is the aforementioned vertex.
            val score0 = 1.0 + ssi * (-1.0 / 0.309016994374947) // Seems slightly faster than 1.0-xsi-ysi-zsi-wsi
            vertexIndex = 0
            if (xsi >= ysi && xsi >= zsi && xsi >= wsi && xsi >= score0) {
                vertexIndex = 1
            } else if (ysi > xsi && ysi >= zsi && ysi >= wsi && ysi >= score0) {
                vertexIndex = 2
            } else if (zsi > xsi && zsi > ysi && zsi >= wsi && zsi >= score0) {
                vertexIndex = 4
            } else if (wsi > xsi && wsi > ysi && wsi > zsi && wsi >= score0) {
                vertexIndex = 8
            }
        }
        return value
    }

    companion object {
        private const val PSIZE = 2048
        private const val PMASK = 2047

        private fun fastFloor(x: Double): Int {
            val xi = x.toInt()
            return if (x < xi) xi - 1 else xi
        }

        private val LOOKUP_2D: Array<LatticePoint2D?> = arrayOfNulls(4)
        private val LOOKUP_3D: Array<LatticePoint3D?> = arrayOfNulls(8)
        private val VERTICES_4D: Array<LatticePoint4D?> = arrayOfNulls(16)
        private const val N2 = 0.01001634121365712
        private const val N3 = 0.030485933181293584
        private const val N4 = 0.009202377986303158
        private val GRADIENTS_2D: Array<Grad2>
        private val GRADIENTS_3D: Array<Grad3>
        private val GRADIENTS_4D: Array<Grad4>

        init {
            LOOKUP_2D[0] = LatticePoint2D(1, 0)
            LOOKUP_2D[1] = LatticePoint2D(0, 0)
            LOOKUP_2D[2] = LatticePoint2D(1, 1)
            LOOKUP_2D[3] = LatticePoint2D(0, 1)
            for (i in 0..7) {
                val i1 = i shr 0 and 1
                val j1 = i shr 1 and 1
                val k1 = i shr 2 and 1
                val i2 = i1 xor 1
                val j2 = j1 xor 1
                val k2 = k1 xor 1

                // The two points within this octant, one from each of the two cubic half-lattices.
                val c0 = LatticePoint3D(i1, j1, k1, 0)
                val c1 = LatticePoint3D(i1 + i2, j1 + j2, k1 + k2, 1)

                // Each single step away on the first half-lattice.
                val c2 = LatticePoint3D(i1 xor 1, j1, k1, 0)
                val c3 = LatticePoint3D(i1, j1 xor 1, k1, 0)
                val c4 = LatticePoint3D(i1, j1, k1 xor 1, 0)

                // Each single step away on the second half-lattice.
                val c5 = LatticePoint3D(i1 + (i2 xor 1), j1 + j2, k1 + k2, 1)
                val c6 = LatticePoint3D(i1 + i2, j1 + (j2 xor 1), k1 + k2, 1)
                val c7 = LatticePoint3D(i1 + i2, j1 + j2, k1 + (k2 xor 1), 1)

                // First two are guaranteed.
                c0.nextOnSuccess = c1
                c0.nextOnFailure = c0.nextOnSuccess
                c1.nextOnSuccess = c2
                c1.nextOnFailure = c1.nextOnSuccess

                // Once we find one on the first half-lattice, the rest are out.
                // In addition, knowing c2 rules out c5.
                c2.nextOnFailure = c3
                c2.nextOnSuccess = c6
                c3.nextOnFailure = c4
                c3.nextOnSuccess = c5
                c4.nextOnSuccess = c5
                c4.nextOnFailure = c4.nextOnSuccess

                // Once we find one on the second half-lattice, the rest are out.
                c5.nextOnFailure = c6
                c5.nextOnSuccess = null
                c6.nextOnFailure = c7
                c6.nextOnSuccess = null
                c7.nextOnSuccess = null
                c7.nextOnFailure = c7.nextOnSuccess
                LOOKUP_3D[i] = c0
            }
            for (i in 0..15) {
                VERTICES_4D[i] = LatticePoint4D(i shr 0 and 1, i shr 1 and 1, i shr 2 and 1, i shr 3 and 1)
            }
        }

        init {
            val grad2 = arrayOf(
                Grad2(0.130526192220052, 0.99144486137381),
                Grad2(0.38268343236509, 0.923879532511287),
                Grad2(0.608761429008721, 0.793353340291235),
                Grad2(0.793353340291235, 0.608761429008721),
                Grad2(0.923879532511287, 0.38268343236509),
                Grad2(0.99144486137381, 0.130526192220051),
                Grad2(0.99144486137381, -0.130526192220051),
                Grad2(0.923879532511287, -0.38268343236509),
                Grad2(0.793353340291235, -0.60876142900872),
                Grad2(0.608761429008721, -0.793353340291235),
                Grad2(0.38268343236509, -0.923879532511287),
                Grad2(0.130526192220052, -0.99144486137381),
                Grad2(-0.130526192220052, -0.99144486137381),
                Grad2(-0.38268343236509, -0.923879532511287),
                Grad2(-0.608761429008721, -0.793353340291235),
                Grad2(-0.793353340291235, -0.608761429008721),
                Grad2(-0.923879532511287, -0.38268343236509),
                Grad2(-0.99144486137381, -0.130526192220052),
                Grad2(-0.99144486137381, 0.130526192220051),
                Grad2(-0.923879532511287, 0.38268343236509),
                Grad2(-0.793353340291235, 0.608761429008721),
                Grad2(-0.608761429008721, 0.793353340291235),
                Grad2(-0.38268343236509, 0.923879532511287),
                Grad2(-0.130526192220052, 0.99144486137381))
            for (i in grad2.indices) {
                grad2[i].dx /= N2
                grad2[i].dy /= N2
            }
            GRADIENTS_2D = Array(PSIZE) { grad2[it % grad2.size] }
            val grad3 = arrayOf(
                Grad3(-2.22474487139, -2.22474487139, -1.0),
                Grad3(-2.22474487139, -2.22474487139, 1.0),
                Grad3(-3.0862664687972017, -1.1721513422464978, 0.0),
                Grad3(-1.1721513422464978, -3.0862664687972017, 0.0),
                Grad3(-2.22474487139, -1.0, -2.22474487139),
                Grad3(-2.22474487139, 1.0, -2.22474487139),
                Grad3(-1.1721513422464978, 0.0, -3.0862664687972017),
                Grad3(-3.0862664687972017, 0.0, -1.1721513422464978),
                Grad3(-2.22474487139, -1.0, 2.22474487139),
                Grad3(-2.22474487139, 1.0, 2.22474487139),
                Grad3(-3.0862664687972017, 0.0, 1.1721513422464978),
                Grad3(-1.1721513422464978, 0.0, 3.0862664687972017),
                Grad3(-2.22474487139, 2.22474487139, -1.0),
                Grad3(-2.22474487139, 2.22474487139, 1.0),
                Grad3(-1.1721513422464978, 3.0862664687972017, 0.0),
                Grad3(-3.0862664687972017, 1.1721513422464978, 0.0),
                Grad3(-1.0, -2.22474487139, -2.22474487139),
                Grad3(1.0, -2.22474487139, -2.22474487139),
                Grad3(0.0, -3.0862664687972017, -1.1721513422464978),
                Grad3(0.0, -1.1721513422464978, -3.0862664687972017),
                Grad3(-1.0, -2.22474487139, 2.22474487139),
                Grad3(1.0, -2.22474487139, 2.22474487139),
                Grad3(0.0, -1.1721513422464978, 3.0862664687972017),
                Grad3(0.0, -3.0862664687972017, 1.1721513422464978),
                Grad3(-1.0, 2.22474487139, -2.22474487139),
                Grad3(1.0, 2.22474487139, -2.22474487139),
                Grad3(0.0, 1.1721513422464978, -3.0862664687972017),
                Grad3(0.0, 3.0862664687972017, -1.1721513422464978),
                Grad3(-1.0, 2.22474487139, 2.22474487139),
                Grad3(1.0, 2.22474487139, 2.22474487139),
                Grad3(0.0, 3.0862664687972017, 1.1721513422464978),
                Grad3(0.0, 1.1721513422464978, 3.0862664687972017),
                Grad3(2.22474487139, -2.22474487139, -1.0),
                Grad3(2.22474487139, -2.22474487139, 1.0),
                Grad3(1.1721513422464978, -3.0862664687972017, 0.0),
                Grad3(3.0862664687972017, -1.1721513422464978, 0.0),
                Grad3(2.22474487139, -1.0, -2.22474487139),
                Grad3(2.22474487139, 1.0, -2.22474487139),
                Grad3(3.0862664687972017, 0.0, -1.1721513422464978),
                Grad3(1.1721513422464978, 0.0, -3.0862664687972017),
                Grad3(2.22474487139, -1.0, 2.22474487139),
                Grad3(2.22474487139, 1.0, 2.22474487139),
                Grad3(1.1721513422464978, 0.0, 3.0862664687972017),
                Grad3(3.0862664687972017, 0.0, 1.1721513422464978),
                Grad3(2.22474487139, 2.22474487139, -1.0),
                Grad3(2.22474487139, 2.22474487139, 1.0),
                Grad3(3.0862664687972017, 1.1721513422464978, 0.0),
                Grad3(1.1721513422464978, 3.0862664687972017, 0.0))
            for (i in grad3.indices) {
                grad3[i].dx /= N3
                grad3[i].dy /= N3
                grad3[i].dz /= N3
            }
            GRADIENTS_3D = Array(PSIZE) { grad3[it % grad3.size] }
            val grad4 = arrayOf(
                Grad4(-0.753341017856078, -0.37968289875261624, -0.37968289875261624, -0.37968289875261624),
                Grad4(-0.7821684431180708, -0.4321472685365301, -0.4321472685365301, 0.12128480194602098),
                Grad4(-0.7821684431180708, -0.4321472685365301, 0.12128480194602098, -0.4321472685365301),
                Grad4(-0.7821684431180708, 0.12128480194602098, -0.4321472685365301, -0.4321472685365301),
                Grad4(-0.8586508742123365, -0.508629699630796, 0.044802370851755174, 0.044802370851755174),
                Grad4(-0.8586508742123365, 0.044802370851755174, -0.508629699630796, 0.044802370851755174),
                Grad4(-0.8586508742123365, 0.044802370851755174, 0.044802370851755174, -0.508629699630796),
                Grad4(-0.9982828964265062, -0.03381941603233842, -0.03381941603233842, -0.03381941603233842),
                Grad4(-0.37968289875261624, -0.753341017856078, -0.37968289875261624, -0.37968289875261624),
                Grad4(-0.4321472685365301, -0.7821684431180708, -0.4321472685365301, 0.12128480194602098),
                Grad4(-0.4321472685365301, -0.7821684431180708, 0.12128480194602098, -0.4321472685365301),
                Grad4(0.12128480194602098, -0.7821684431180708, -0.4321472685365301, -0.4321472685365301),
                Grad4(-0.508629699630796, -0.8586508742123365, 0.044802370851755174, 0.044802370851755174),
                Grad4(0.044802370851755174, -0.8586508742123365, -0.508629699630796, 0.044802370851755174),
                Grad4(0.044802370851755174, -0.8586508742123365, 0.044802370851755174, -0.508629699630796),
                Grad4(-0.03381941603233842, -0.9982828964265062, -0.03381941603233842, -0.03381941603233842),
                Grad4(-0.37968289875261624, -0.37968289875261624, -0.753341017856078, -0.37968289875261624),
                Grad4(-0.4321472685365301, -0.4321472685365301, -0.7821684431180708, 0.12128480194602098),
                Grad4(-0.4321472685365301, 0.12128480194602098, -0.7821684431180708, -0.4321472685365301),
                Grad4(0.12128480194602098, -0.4321472685365301, -0.7821684431180708, -0.4321472685365301),
                Grad4(-0.508629699630796, 0.044802370851755174, -0.8586508742123365, 0.044802370851755174),
                Grad4(0.044802370851755174, -0.508629699630796, -0.8586508742123365, 0.044802370851755174),
                Grad4(0.044802370851755174, 0.044802370851755174, -0.8586508742123365, -0.508629699630796),
                Grad4(-0.03381941603233842, -0.03381941603233842, -0.9982828964265062, -0.03381941603233842),
                Grad4(-0.37968289875261624, -0.37968289875261624, -0.37968289875261624, -0.753341017856078),
                Grad4(-0.4321472685365301, -0.4321472685365301, 0.12128480194602098, -0.7821684431180708),
                Grad4(-0.4321472685365301, 0.12128480194602098, -0.4321472685365301, -0.7821684431180708),
                Grad4(0.12128480194602098, -0.4321472685365301, -0.4321472685365301, -0.7821684431180708),
                Grad4(-0.508629699630796, 0.044802370851755174, 0.044802370851755174, -0.8586508742123365),
                Grad4(0.044802370851755174, -0.508629699630796, 0.044802370851755174, -0.8586508742123365),
                Grad4(0.044802370851755174, 0.044802370851755174, -0.508629699630796, -0.8586508742123365),
                Grad4(-0.03381941603233842, -0.03381941603233842, -0.03381941603233842, -0.9982828964265062),
                Grad4(-0.6740059517812944, -0.3239847771997537, -0.3239847771997537, 0.5794684678643381),
                Grad4(-0.7504883828755602, -0.4004672082940195, 0.15296486218853164, 0.5029860367700724),
                Grad4(-0.7504883828755602, 0.15296486218853164, -0.4004672082940195, 0.5029860367700724),
                Grad4(-0.8828161875373585, 0.08164729285680945, 0.08164729285680945, 0.4553054119602712),
                Grad4(-0.4553054119602712, -0.08164729285680945, -0.08164729285680945, 0.8828161875373585),
                Grad4(-0.5029860367700724, -0.15296486218853164, 0.4004672082940195, 0.7504883828755602),
                Grad4(-0.5029860367700724, 0.4004672082940195, -0.15296486218853164, 0.7504883828755602),
                Grad4(-0.5794684678643381, 0.3239847771997537, 0.3239847771997537, 0.6740059517812944),
                Grad4(-0.3239847771997537, -0.6740059517812944, -0.3239847771997537, 0.5794684678643381),
                Grad4(-0.4004672082940195, -0.7504883828755602, 0.15296486218853164, 0.5029860367700724),
                Grad4(0.15296486218853164, -0.7504883828755602, -0.4004672082940195, 0.5029860367700724),
                Grad4(0.08164729285680945, -0.8828161875373585, 0.08164729285680945, 0.4553054119602712),
                Grad4(-0.08164729285680945, -0.4553054119602712, -0.08164729285680945, 0.8828161875373585),
                Grad4(-0.15296486218853164, -0.5029860367700724, 0.4004672082940195, 0.7504883828755602),
                Grad4(0.4004672082940195, -0.5029860367700724, -0.15296486218853164, 0.7504883828755602),
                Grad4(0.3239847771997537, -0.5794684678643381, 0.3239847771997537, 0.6740059517812944),
                Grad4(-0.3239847771997537, -0.3239847771997537, -0.6740059517812944, 0.5794684678643381),
                Grad4(-0.4004672082940195, 0.15296486218853164, -0.7504883828755602, 0.5029860367700724),
                Grad4(0.15296486218853164, -0.4004672082940195, -0.7504883828755602, 0.5029860367700724),
                Grad4(0.08164729285680945, 0.08164729285680945, -0.8828161875373585, 0.4553054119602712),
                Grad4(-0.08164729285680945, -0.08164729285680945, -0.4553054119602712, 0.8828161875373585),
                Grad4(-0.15296486218853164, 0.4004672082940195, -0.5029860367700724, 0.7504883828755602),
                Grad4(0.4004672082940195, -0.15296486218853164, -0.5029860367700724, 0.7504883828755602),
                Grad4(0.3239847771997537, 0.3239847771997537, -0.5794684678643381, 0.6740059517812944),
                Grad4(-0.6740059517812944, -0.3239847771997537, 0.5794684678643381, -0.3239847771997537),
                Grad4(-0.7504883828755602, -0.4004672082940195, 0.5029860367700724, 0.15296486218853164),
                Grad4(-0.7504883828755602, 0.15296486218853164, 0.5029860367700724, -0.4004672082940195),
                Grad4(-0.8828161875373585, 0.08164729285680945, 0.4553054119602712, 0.08164729285680945),
                Grad4(-0.4553054119602712, -0.08164729285680945, 0.8828161875373585, -0.08164729285680945),
                Grad4(-0.5029860367700724, -0.15296486218853164, 0.7504883828755602, 0.4004672082940195),
                Grad4(-0.5029860367700724, 0.4004672082940195, 0.7504883828755602, -0.15296486218853164),
                Grad4(-0.5794684678643381, 0.3239847771997537, 0.6740059517812944, 0.3239847771997537),
                Grad4(-0.3239847771997537, -0.6740059517812944, 0.5794684678643381, -0.3239847771997537),
                Grad4(-0.4004672082940195, -0.7504883828755602, 0.5029860367700724, 0.15296486218853164),
                Grad4(0.15296486218853164, -0.7504883828755602, 0.5029860367700724, -0.4004672082940195),
                Grad4(0.08164729285680945, -0.8828161875373585, 0.4553054119602712, 0.08164729285680945),
                Grad4(-0.08164729285680945, -0.4553054119602712, 0.8828161875373585, -0.08164729285680945),
                Grad4(-0.15296486218853164, -0.5029860367700724, 0.7504883828755602, 0.4004672082940195),
                Grad4(0.4004672082940195, -0.5029860367700724, 0.7504883828755602, -0.15296486218853164),
                Grad4(0.3239847771997537, -0.5794684678643381, 0.6740059517812944, 0.3239847771997537),
                Grad4(-0.3239847771997537, -0.3239847771997537, 0.5794684678643381, -0.6740059517812944),
                Grad4(-0.4004672082940195, 0.15296486218853164, 0.5029860367700724, -0.7504883828755602),
                Grad4(0.15296486218853164, -0.4004672082940195, 0.5029860367700724, -0.7504883828755602),
                Grad4(0.08164729285680945, 0.08164729285680945, 0.4553054119602712, -0.8828161875373585),
                Grad4(-0.08164729285680945, -0.08164729285680945, 0.8828161875373585, -0.4553054119602712),
                Grad4(-0.15296486218853164, 0.4004672082940195, 0.7504883828755602, -0.5029860367700724),
                Grad4(0.4004672082940195, -0.15296486218853164, 0.7504883828755602, -0.5029860367700724),
                Grad4(0.3239847771997537, 0.3239847771997537, 0.6740059517812944, -0.5794684678643381),
                Grad4(-0.6740059517812944, 0.5794684678643381, -0.3239847771997537, -0.3239847771997537),
                Grad4(-0.7504883828755602, 0.5029860367700724, -0.4004672082940195, 0.15296486218853164),
                Grad4(-0.7504883828755602, 0.5029860367700724, 0.15296486218853164, -0.4004672082940195),
                Grad4(-0.8828161875373585, 0.4553054119602712, 0.08164729285680945, 0.08164729285680945),
                Grad4(-0.4553054119602712, 0.8828161875373585, -0.08164729285680945, -0.08164729285680945),
                Grad4(-0.5029860367700724, 0.7504883828755602, -0.15296486218853164, 0.4004672082940195),
                Grad4(-0.5029860367700724, 0.7504883828755602, 0.4004672082940195, -0.15296486218853164),
                Grad4(-0.5794684678643381, 0.6740059517812944, 0.3239847771997537, 0.3239847771997537),
                Grad4(-0.3239847771997537, 0.5794684678643381, -0.6740059517812944, -0.3239847771997537),
                Grad4(-0.4004672082940195, 0.5029860367700724, -0.7504883828755602, 0.15296486218853164),
                Grad4(0.15296486218853164, 0.5029860367700724, -0.7504883828755602, -0.4004672082940195),
                Grad4(0.08164729285680945, 0.4553054119602712, -0.8828161875373585, 0.08164729285680945),
                Grad4(-0.08164729285680945, 0.8828161875373585, -0.4553054119602712, -0.08164729285680945),
                Grad4(-0.15296486218853164, 0.7504883828755602, -0.5029860367700724, 0.4004672082940195),
                Grad4(0.4004672082940195, 0.7504883828755602, -0.5029860367700724, -0.15296486218853164),
                Grad4(0.3239847771997537, 0.6740059517812944, -0.5794684678643381, 0.3239847771997537),
                Grad4(-0.3239847771997537, 0.5794684678643381, -0.3239847771997537, -0.6740059517812944),
                Grad4(-0.4004672082940195, 0.5029860367700724, 0.15296486218853164, -0.7504883828755602),
                Grad4(0.15296486218853164, 0.5029860367700724, -0.4004672082940195, -0.7504883828755602),
                Grad4(0.08164729285680945, 0.4553054119602712, 0.08164729285680945, -0.8828161875373585),
                Grad4(-0.08164729285680945, 0.8828161875373585, -0.08164729285680945, -0.4553054119602712),
                Grad4(-0.15296486218853164, 0.7504883828755602, 0.4004672082940195, -0.5029860367700724),
                Grad4(0.4004672082940195, 0.7504883828755602, -0.15296486218853164, -0.5029860367700724),
                Grad4(0.3239847771997537, 0.6740059517812944, 0.3239847771997537, -0.5794684678643381),
                Grad4(0.5794684678643381, -0.6740059517812944, -0.3239847771997537, -0.3239847771997537),
                Grad4(0.5029860367700724, -0.7504883828755602, -0.4004672082940195, 0.15296486218853164),
                Grad4(0.5029860367700724, -0.7504883828755602, 0.15296486218853164, -0.4004672082940195),
                Grad4(0.4553054119602712, -0.8828161875373585, 0.08164729285680945, 0.08164729285680945),
                Grad4(0.8828161875373585, -0.4553054119602712, -0.08164729285680945, -0.08164729285680945),
                Grad4(0.7504883828755602, -0.5029860367700724, -0.15296486218853164, 0.4004672082940195),
                Grad4(0.7504883828755602, -0.5029860367700724, 0.4004672082940195, -0.15296486218853164),
                Grad4(0.6740059517812944, -0.5794684678643381, 0.3239847771997537, 0.3239847771997537),
                Grad4(0.5794684678643381, -0.3239847771997537, -0.6740059517812944, -0.3239847771997537),
                Grad4(0.5029860367700724, -0.4004672082940195, -0.7504883828755602, 0.15296486218853164),
                Grad4(0.5029860367700724, 0.15296486218853164, -0.7504883828755602, -0.4004672082940195),
                Grad4(0.4553054119602712, 0.08164729285680945, -0.8828161875373585, 0.08164729285680945),
                Grad4(0.8828161875373585, -0.08164729285680945, -0.4553054119602712, -0.08164729285680945),
                Grad4(0.7504883828755602, -0.15296486218853164, -0.5029860367700724, 0.4004672082940195),
                Grad4(0.7504883828755602, 0.4004672082940195, -0.5029860367700724, -0.15296486218853164),
                Grad4(0.6740059517812944, 0.3239847771997537, -0.5794684678643381, 0.3239847771997537),
                Grad4(0.5794684678643381, -0.3239847771997537, -0.3239847771997537, -0.6740059517812944),
                Grad4(0.5029860367700724, -0.4004672082940195, 0.15296486218853164, -0.7504883828755602),
                Grad4(0.5029860367700724, 0.15296486218853164, -0.4004672082940195, -0.7504883828755602),
                Grad4(0.4553054119602712, 0.08164729285680945, 0.08164729285680945, -0.8828161875373585),
                Grad4(0.8828161875373585, -0.08164729285680945, -0.08164729285680945, -0.4553054119602712),
                Grad4(0.7504883828755602, -0.15296486218853164, 0.4004672082940195, -0.5029860367700724),
                Grad4(0.7504883828755602, 0.4004672082940195, -0.15296486218853164, -0.5029860367700724),
                Grad4(0.6740059517812944, 0.3239847771997537, 0.3239847771997537, -0.5794684678643381),
                Grad4(0.03381941603233842, 0.03381941603233842, 0.03381941603233842, 0.9982828964265062),
                Grad4(-0.044802370851755174, -0.044802370851755174, 0.508629699630796, 0.8586508742123365),
                Grad4(-0.044802370851755174, 0.508629699630796, -0.044802370851755174, 0.8586508742123365),
                Grad4(-0.12128480194602098, 0.4321472685365301, 0.4321472685365301, 0.7821684431180708),
                Grad4(0.508629699630796, -0.044802370851755174, -0.044802370851755174, 0.8586508742123365),
                Grad4(0.4321472685365301, -0.12128480194602098, 0.4321472685365301, 0.7821684431180708),
                Grad4(0.4321472685365301, 0.4321472685365301, -0.12128480194602098, 0.7821684431180708),
                Grad4(0.37968289875261624, 0.37968289875261624, 0.37968289875261624, 0.753341017856078),
                Grad4(0.03381941603233842, 0.03381941603233842, 0.9982828964265062, 0.03381941603233842),
                Grad4(-0.044802370851755174, 0.044802370851755174, 0.8586508742123365, 0.508629699630796),
                Grad4(-0.044802370851755174, 0.508629699630796, 0.8586508742123365, -0.044802370851755174),
                Grad4(-0.12128480194602098, 0.4321472685365301, 0.7821684431180708, 0.4321472685365301),
                Grad4(0.508629699630796, -0.044802370851755174, 0.8586508742123365, -0.044802370851755174),
                Grad4(0.4321472685365301, -0.12128480194602098, 0.7821684431180708, 0.4321472685365301),
                Grad4(0.4321472685365301, 0.4321472685365301, 0.7821684431180708, -0.12128480194602098),
                Grad4(0.37968289875261624, 0.37968289875261624, 0.753341017856078, 0.37968289875261624),
                Grad4(0.03381941603233842, 0.9982828964265062, 0.03381941603233842, 0.03381941603233842),
                Grad4(-0.044802370851755174, 0.8586508742123365, -0.044802370851755174, 0.508629699630796),
                Grad4(-0.044802370851755174, 0.8586508742123365, 0.508629699630796, -0.044802370851755174),
                Grad4(-0.12128480194602098, 0.7821684431180708, 0.4321472685365301, 0.4321472685365301),
                Grad4(0.508629699630796, 0.8586508742123365, -0.044802370851755174, -0.044802370851755174),
                Grad4(0.4321472685365301, 0.7821684431180708, -0.12128480194602098, 0.4321472685365301),
                Grad4(0.4321472685365301, 0.7821684431180708, 0.4321472685365301, -0.12128480194602098),
                Grad4(0.37968289875261624, 0.753341017856078, 0.37968289875261624, 0.37968289875261624),
                Grad4(0.9982828964265062, 0.03381941603233842, 0.03381941603233842, 0.03381941603233842),
                Grad4(0.8586508742123365, -0.044802370851755174, -0.044802370851755174, 0.508629699630796),
                Grad4(0.8586508742123365, -0.044802370851755174, 0.508629699630796, -0.044802370851755174),
                Grad4(0.7821684431180708, -0.12128480194602098, 0.4321472685365301, 0.4321472685365301),
                Grad4(0.8586508742123365, 0.508629699630796, -0.044802370851755174, -0.044802370851755174),
                Grad4(0.7821684431180708, 0.4321472685365301, -0.12128480194602098, 0.4321472685365301),
                Grad4(0.7821684431180708, 0.4321472685365301, 0.4321472685365301, -0.12128480194602098),
                Grad4(0.753341017856078, 0.37968289875261624, 0.37968289875261624, 0.37968289875261624))
            for (i in grad4.indices) {
                grad4[i].dx /= N4
                grad4[i].dy /= N4
                grad4[i].dz /= N4
                grad4[i].dw /= N4
            }
            GRADIENTS_4D = Array(PSIZE) { grad4[it % grad4.size] }
        }
    }

    private class LatticePoint2D(var xsv: Int, var ysv: Int) {
        var dx: Double
        var dy: Double

        init {
            val ssv = (xsv + ysv) * -0.211324865405187
            dx = -xsv - ssv
            dy = -ysv - ssv
        }
    }

    private class LatticePoint3D(xrv: Int, yrv: Int, zrv: Int, lattice: Int) {
        var dxr: Double
        var dyr: Double
        var dzr: Double
        var xrv: Int
        var yrv: Int
        var zrv: Int
        var nextOnFailure: LatticePoint3D? = null
        var nextOnSuccess: LatticePoint3D? = null

        init {
            dxr = -xrv + lattice * 0.5
            dyr = -yrv + lattice * 0.5
            dzr = -zrv + lattice * 0.5
            this.xrv = xrv + lattice * 1024
            this.yrv = yrv + lattice * 1024
            this.zrv = zrv + lattice * 1024
        }
    }

    private class LatticePoint4D(xsv: Int, ysv: Int, zsv: Int, wsv: Int) {
        var xsv: Int
        var ysv: Int
        var zsv: Int
        var wsv: Int
        var dx: Double
        var dy: Double
        var dz: Double
        var dw: Double
        var xsi: Double
        var ysi: Double
        var zsi: Double
        var wsi: Double
        var ssiDelta: Double

        init {
            this.xsv = xsv + 409
            this.ysv = ysv + 409
            this.zsv = zsv + 409
            this.wsv = wsv + 409
            val ssv = (xsv + ysv + zsv + wsv) * 0.309016994374947
            dx = -xsv - ssv
            dy = -ysv - ssv
            dz = -zsv - ssv
            dw = -wsv - ssv
            xsi = 0.2 - xsv
            ysi = 0.2 - ysv
            zsi = 0.2 - zsv
            wsi = 0.2 - wsv
            ssiDelta = (0.8 - xsv - ysv - zsv - wsv) * 0.309016994374947
        }
    }

    private class Grad2(var dx: Double, var dy: Double)
    private class Grad3(var dx: Double, var dy: Double, var dz: Double)
    private class Grad4(var dx: Double, var dy: Double, var dz: Double, var dw: Double)

    init {
        var seed = seed
        perm = ShortArray(PSIZE)
        permGrad2 = arrayOfNulls(PSIZE)
        permGrad3 = arrayOfNulls(PSIZE)
        permGrad4 = arrayOfNulls(PSIZE)
        val source = ShortArray(PSIZE)
        for (i in 0 until PSIZE) source[i] = i.toShort()
        for (i in PSIZE - 1 downTo 0) {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            var r = ((seed + 31) % (i + 1)).toInt()
            if (r < 0) r += i + 1
            perm[i] = source[r]
            permGrad2[i] = GRADIENTS_2D[perm[i].toInt()]
            permGrad3[i] = GRADIENTS_3D[perm[i].toInt()]
            permGrad4[i] = GRADIENTS_4D[perm[i].toInt()]
            source[r] = source[i]
        }
    }
}