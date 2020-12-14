package posidon.uranium.graphics

import posidon.library.types.Vec2f
import posidon.library.types.Vec3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

open class Matrix4f(
    val all: FloatArray = FloatArray(SIZE * SIZE)
) {

    constructor(mat: Matrix4f) : this(mat.all.clone())

    operator fun get(x: Int, y: Int) = all[y * SIZE + x]
    operator fun set(x: Int, y: Int, value: Float) { all[y * SIZE + x] = value }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix4f) return false
        return all.contentEquals(other.all)
    }

    override fun hashCode(): Int = all.contentHashCode()

    operator fun times(other: Matrix4f): Matrix4f {
        val result = identity()
        for (i in 0 until SIZE) for (j in 0 until SIZE)
            result[i, j] =
                this[i, 0] * other[0, j] +
                this[i, 1] * other[1, j] +
                this[i, 2] * other[2, j] +
                this[i, 3] * other[3, j]
        return result
    }

    override fun toString(): String {
        return "Matrix4f { " + all.joinToString() + " }"
    }


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

    fun inverse(): Matrix4f {
        val m = Matrix4f(this)
        m.invert()
        return m
    }

    companion object {

        const val SIZE = 4

        fun identity(): Matrix4f {
            val result = Matrix4f()
            for (i in 0 until SIZE) for (j in 0 until SIZE) result[i, j] = 0f
            result[0, 0] = 1f
            result[1, 1] = 1f
            result[2, 2] = 1f
            result[3, 3] = 1f
            return result
        }

        fun translate(translation: Vec3f): Matrix4f {
            val result = identity()
            result[3, 0] = translation.x
            result[3, 1] = translation.y
            result[3, 2] = translation.z
            return result
        }

        fun translate(translation: Vec2f): Matrix4f {
            val result = identity()
            result[3, 0] = translation.x
            result[3, 1] = translation.y
            result[3, 2] = 0f
            return result
        }

        fun rotateX(radians: Double): Matrix4f {
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


        fun rotateY(radians: Double): Matrix4f {
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

        fun rotateZ(radians: Double): Matrix4f {
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

        fun scale(scale: Vec3f): Matrix4f {
            val result = identity()
            result[0, 0] = scale.x
            result[1, 1] = scale.y
            result[2, 2] = scale.z
            return result
        }

        fun transform(position: Vec3f, rotation: Vec3f, scale: Vec3f): Matrix4f {
            val rotX = rotateX(Math.toRadians(rotation.x.toDouble()))
            val rotY = rotateY(Math.toRadians(rotation.y.toDouble()))
            val rotZ = rotateZ(Math.toRadians(rotation.z.toDouble()))
            return (rotX * rotY * rotZ) * scale(scale) * translate(position)
        }

        fun projection(fov: Float, aspectRatio: Float, near: Float, far: Float): Matrix4f {
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

        fun view(position: Vec3f, rotation: Vec2f): Matrix4f {
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
