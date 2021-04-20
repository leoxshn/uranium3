package io.posidon.library.types

import io.posidon.library.util.invSqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

open class Mat4f(
    val all: FloatArray = FloatArray(SIZE * SIZE)
) {

    constructor(mat: Mat4f) : this(mat.all.clone())

    operator fun get(x: Int, y: Int) = all[y * SIZE + x]
    operator fun set(x: Int, y: Int, value: Float) { all[y * SIZE + x] = value }
    fun set(other: Mat4f) { for (i in all.indices) all[i] = other.all[i] }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mat4f) return false
        return all.contentEquals(other.all)
    }

    override fun hashCode(): Int = all.contentHashCode()

    operator fun times(other: Mat4f): Mat4f {
        val result = identity()
        for (i in 0 until SIZE) for (j in 0 until SIZE)
            result[i, j] =
                this[i, 0] * other[0, j] +
                this[i, 1] * other[1, j] +
                this[i, 2] * other[2, j] +
                this[i, 3] * other[3, j]
        return result
    }

    fun selfMultiply(other: Mat4f) = set(this * other)

    operator fun times(vec: Vec4f): Vec4f = Vec4f(
        this[0, 0] * vec.x +
        this[1, 0] * vec.y +
        this[2, 0] * vec.z +
        this[3, 0] * vec.w,
        this[0, 1] * vec.x +
        this[1, 1] * vec.y +
        this[2, 1] * vec.z +
        this[3, 1] * vec.w,
        this[0, 2] * vec.x +
        this[1, 2] * vec.y +
        this[2, 2] * vec.z +
        this[3, 2] * vec.w,
        this[0, 3] * vec.x +
        this[1, 3] * vec.y +
        this[2, 3] * vec.z +
        this[3, 3] * vec.w)


    override fun toString(): String {
        return "Matrix4f { " + all.joinToString() + " }"
    }

    inline fun transposed(): Mat4f {
        val transposedMatrix = Mat4f()
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                transposedMatrix[j, i] = this[i, j]
            }
        }
        return transposedMatrix
    }
/*
    inline fun changeSign(i: Int) = if (i % 2 == 0) 1 else -1

    fun determinant(): Float {
        var sum = 0f
        for (i in 0 until SIZE) {
            sum += changeSign(i) * this[0, i] * createSubMatrix( 0, i).determinant()
        }
        return sum
    }

    inline fun createSubMatrix(excluding_row: Int, excluding_col: Int): Matrix4f {
        val mat = Matrix4f(SIZE - 1, SIZE - 1)
        var r = -1
        for (i in 0 until SIZE) {
            if (i == excluding_row) continue
            r++
            var c = -1
            for (j in 0 until SIZE) {
                if (j == excluding_col) continue
                mat[r, ++c] = this[i, j]
            }
        }
        return mat
    }

    inline fun cofactor(): Matrix4f {
        val mat = Matrix4f()
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                mat[i, j] = changeSign(i) * changeSign(j) * createSubMatrix(i, j).determinant()
            }
        }
        return mat
    }

    inline fun inverse(): Matrix4f {
        return cofactor().transposed().multiplyByConstant(1f / determinant())
    }*/

    /**
     * Invert this 4x4 matrix.
     */
    fun invert() {

        val tmp = FloatArray(12)
        val src = FloatArray(16)
        val dst = FloatArray(16)

        // Transpose matrix
        for (i in 0..3) {
            src[i + 0] = all[i * 4 + 0]
            src[i + 4] = all[i * 4 + 1]
            src[i + 8] = all[i * 4 + 2]
            src[i + 12] = all[i * 4 + 3]
        }

        // Calculate pairs for first 8 elements (cofactors)
        tmp[0] = src[10] * src[15]
        tmp[1] = src[11] * src[14]
        tmp[2] = src[9] * src[15]
        tmp[3] = src[11] * src[13]
        tmp[4] = src[9] * src[14]
        tmp[5] = src[10] * src[13]
        tmp[6] = src[8] * src[15]
        tmp[7] = src[11] * src[12]
        tmp[8] = src[8] * src[14]
        tmp[9] = src[10] * src[12]
        tmp[10] = src[8] * src[13]
        tmp[11] = src[9] * src[12]

        // Calculate first 8 elements (cofactors)
        dst[0] = tmp[0] * src[5] + tmp[3] * src[6] + tmp[4] * src[7]
        dst[0] -= tmp[1] * src[5] + tmp[2] * src[6] + tmp[5] * src[7]
        dst[1] = tmp[1] * src[4] + tmp[6] * src[6] + tmp[9] * src[7]
        dst[1] -= tmp[0] * src[4] + tmp[7] * src[6] + tmp[8] * src[7]
        dst[2] = tmp[2] * src[4] + tmp[7] * src[5] + tmp[10] * src[7]
        dst[2] -= tmp[3] * src[4] + tmp[6] * src[5] + tmp[11] * src[7]
        dst[3] = tmp[5] * src[4] + tmp[8] * src[5] + tmp[11] * src[6]
        dst[3] -= tmp[4] * src[4] + tmp[9] * src[5] + tmp[10] * src[6]
        dst[4] = tmp[1] * src[1] + tmp[2] * src[2] + tmp[5] * src[3]
        dst[4] -= tmp[0] * src[1] + tmp[3] * src[2] + tmp[4] * src[3]
        dst[5] = tmp[0] * src[0] + tmp[7] * src[2] + tmp[8] * src[3]
        dst[5] -= tmp[1] * src[0] + tmp[6] * src[2] + tmp[9] * src[3]
        dst[6] = tmp[3] * src[0] + tmp[6] * src[1] + tmp[11] * src[3]
        dst[6] -= tmp[2] * src[0] + tmp[7] * src[1] + tmp[10] * src[3]
        dst[7] = tmp[4] * src[0] + tmp[9] * src[1] + tmp[10] * src[2]
        dst[7] -= tmp[5] * src[0] + tmp[8] * src[1] + tmp[11] * src[2]

        // Calculate pairs for second 8 elements (cofactors)
        tmp[0] = src[2] * src[7]
        tmp[1] = src[3] * src[6]
        tmp[2] = src[1] * src[7]
        tmp[3] = src[3] * src[5]
        tmp[4] = src[1] * src[6]
        tmp[5] = src[2] * src[5]
        tmp[6] = src[0] * src[7]
        tmp[7] = src[3] * src[4]
        tmp[8] = src[0] * src[6]
        tmp[9] = src[2] * src[4]
        tmp[10] = src[0] * src[5]
        tmp[11] = src[1] * src[4]

        // Calculate second 8 elements (cofactors)
        dst[8] = tmp[0] * src[13] + tmp[3] * src[14] + tmp[4] * src[15]
        dst[8] -= tmp[1] * src[13] + tmp[2] * src[14] + tmp[5] * src[15]
        dst[9] = tmp[1] * src[12] + tmp[6] * src[14] + tmp[9] * src[15]
        dst[9] -= tmp[0] * src[12] + tmp[7] * src[14] + tmp[8] * src[15]
        dst[10] = tmp[2] * src[12] + tmp[7] * src[13] + tmp[10] * src[15]
        dst[10] -= tmp[3] * src[12] + tmp[6] * src[13] + tmp[11] * src[15]
        dst[11] = tmp[5] * src[12] + tmp[8] * src[13] + tmp[11] * src[14]
        dst[11] -= tmp[4] * src[12] + tmp[9] * src[13] + tmp[10] * src[14]
        dst[12] = tmp[2] * src[10] + tmp[5] * src[11] + tmp[1] * src[9]
        dst[12] -= tmp[4] * src[11] + tmp[0] * src[9] + tmp[3] * src[10]
        dst[13] = tmp[8] * src[11] + tmp[0] * src[8] + tmp[7] * src[10]
        dst[13] -= tmp[6] * src[10] + tmp[9] * src[11] + tmp[1] * src[8]
        dst[14] = tmp[6] * src[9] + tmp[11] * src[11] + tmp[3] * src[8]
        dst[14] -= tmp[10] * src[11] + tmp[2] * src[8] + tmp[7] * src[9]
        dst[15] = tmp[10] * src[10] + tmp[4] * src[8] + tmp[9] * src[9]
        dst[15] -= tmp[8] * src[9] + tmp[11] * src[10] + tmp[5] * src[8]

        // Calculate determinant
        var det = src[0] * dst[0] + src[1] * dst[1] + src[2] * dst[2] + src[3] * dst[3]

        // Calculate matrix inverse
        det = 1.0f / det
        for (i in 0..15) all[i] = dst[i] * det
    }

    inline fun inverse(): Mat4f = Mat4f(this).apply { invert() }

    inline fun setIdentity() {
        for (i in 0 until SIZE) for (j in 0 until SIZE) this[i, j] = 0f
        this[0, 0] = 1f
        this[1, 1] = 1f
        this[2, 2] = 1f
        this[3, 3] = 1f
    }

    fun setLookAt(eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float
    ): Mat4f {
        // Compute direction from position to lookAt
        var dirX: Float = eyeX - centerX
        var dirY: Float = eyeY - centerY
        var dirZ: Float = eyeZ - centerZ
        // Normalize direction
        val invDirLength: Float = invSqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        dirX *= invDirLength
        dirY *= invDirLength
        dirZ *= invDirLength
        // left = up x direction
        var leftX: Float = upY * dirZ - upZ * dirY
        var leftY: Float = upZ * dirX - upX * dirZ
        var leftZ: Float = upX * dirY - upY * dirX
        // normalize left
        val invLeftLength: Float = invSqrt(leftX * leftX + leftY * leftY + leftZ * leftZ)
        leftX *= invLeftLength
        leftY *= invLeftLength
        leftZ *= invLeftLength
        // up = direction x left
        val upnX = dirY * leftZ - dirZ * leftY
        val upnY = dirZ * leftX - dirX * leftZ
        val upnZ = dirX * leftY - dirY * leftX
        this[0, 0] = leftX
        this[0, 1] = upnX
        this[0, 2] = dirX
        this[0, 3] = 0f
        this[1, 0] = leftY
        this[1, 1] = upnY
        this[1, 2] = dirY
        this[1, 3] = 0f
        this[2, 0] = leftZ
        this[2, 1] = upnZ
        this[2, 2] = dirZ
        this[2, 3] = 0f
        this[3, 0] = -(leftX * eyeX + leftY * eyeY + leftZ * eyeZ)
        this[3, 1] = -(upnX * eyeX + upnY * eyeY + upnZ * eyeZ)
        this[3, 2] = -(dirX * eyeX + dirY * eyeY + dirZ * eyeZ)
        this[3, 3] = 1f
        return this
    }

    companion object {

        const val SIZE = 4

        inline fun identity(): Mat4f = Mat4f().apply { setIdentity() }

        fun translate(translation: Vec3f): Mat4f {
            val result = identity()
            result[3, 0] = translation.x
            result[3, 1] = translation.y
            result[3, 2] = translation.z
            return result
        }

        fun translate(translation: Vec2f): Mat4f {
            val result = identity()
            result[3, 0] = translation.x
            result[3, 1] = translation.y
            result[3, 2] = 0f
            return result
        }

        fun rotateX(radians: Double): Mat4f {
            val result = identity()
            val cos = cos(radians).toFloat()
            val sin = sin(radians).toFloat()
            val c = 1 - cos
            result[0, 0] = cos + c
            result[0, 1] = 0f
            result[0, 2] = 0f
            result[1, 0] = 0f
            result[1, 1] = cos
            result[1, 2] = -sin
            result[2, 0] = 0f
            result[2, 1] = sin
            result[2, 2] = cos
            return result
        }


        fun rotateY(radians: Double): Mat4f {
            val result = identity()
            val cos = cos(radians).toFloat()
            val sin = sin(radians).toFloat()
            val c = 1 - cos
            result[0, 0] = cos
            result[0, 1] = 0f
            result[0, 2] = sin
            result[1, 0] = 0f
            result[1, 1] = cos + c
            result[1, 2] = 0f
            result[2, 0] = -sin
            result[2, 1] = 0f
            result[2, 2] = cos
            return result
        }

        fun rotateZ(radians: Double): Mat4f {
            val result = identity()
            val cos = cos(radians).toFloat()
            val sin = sin(radians).toFloat()
            val c = 1 - cos
            result[0, 0] = cos
            result[0, 1] = -sin
            result[0, 2] = 0f
            result[1, 0] = sin
            result[1, 1] = cos
            result[1, 2] = 0f
            result[2, 0] = 0f
            result[2, 1] = 0f
            result[2, 2] = cos + c
            return result
        }

        fun scale(scale: Vec3f): Mat4f {
            val result = identity()
            result[0, 0] = scale.x
            result[1, 1] = scale.y
            result[2, 2] = scale.z
            return result
        }

        fun transform(position: Vec3f, rotation: Vec3f, scale: Vec3f): Mat4f {
            val rotX = rotateX(Math.toRadians(rotation.x.toDouble()))
            val rotY = rotateY(Math.toRadians(rotation.y.toDouble()))
            val rotZ = rotateZ(Math.toRadians(rotation.z.toDouble()))
            return (rotX * rotY * rotZ) * scale(scale) * translate(position)
        }

        fun projection(fov: Float, aspectRatio: Float, near: Float, far: Float): Mat4f {
            val result = identity()
            val tanfov = tan(Math.toRadians(fov / 2.toDouble())).toFloat()
            val range = far - near
            result[0, 0] = 1f / (aspectRatio * tanfov)
            result[1, 1] = 1f / tanfov
            result[2, 2] = -(far + near) / range
            result[2, 3] = -1f
            result[3, 2] = -(2 * far * near) / range
            result[3, 3] = 0f
            return result
        }

        fun view(position: Vec3f, rotation: Vec2f): Mat4f {
            val translation = identity().apply {
                this[3, 0] = -position.x
                this[3, 1] = -position.y
                this[3, 2] = -position.z
            }
            val rotX = rotateX(Math.toRadians(rotation.x.toDouble()))
            val rotY = rotateY(Math.toRadians(rotation.y.toDouble()))
            return translation * (rotY * rotX)
        }
    }
}
