package posidon.uranium.graphics

import posidon.library.types.Vec2f
import posidon.library.types.Vec3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

open class Matrix4f {

    val all = FloatArray(SIZE * SIZE)

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