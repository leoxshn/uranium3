package posidon.library.types

import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Vec3f(var x: Float, var y: Float, var z: Float) {

    inline fun set(v: Vec3f) = set(v.x, v.y, v.z)
    inline fun set(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3f) return false
        return other.x.compareTo(x) == 0 && other.y.compareTo(y) == 0 && other.z.compareTo(z) == 0
    }

    override fun hashCode() = Objects.hash(x, y, z)
    override fun toString() = "vec3f($x, $y, $z)"
    inline fun toVec3i() = Vec3i(x.toInt(), y.toInt(), z.toInt())
    inline fun roundToVec3i() = Vec3i(x.roundToInt(), y.roundToInt(), z.roundToInt())

    companion object {
        fun blend(v1: Vec3f, v2: Vec3f, ratio: Float): Vec3f {
            val inverseRation = 1f - ratio
            val x = v1.x * ratio + v2.x * inverseRation
            val y = v1.y * ratio + v2.y * inverseRation
            val z = v1.z * ratio + v2.z * inverseRation
            return Vec3f(x, y, z)
        }

        inline fun zero() = Vec3f(0f, 0f, 0f)
        val ZERO = zero()
    }

    inline val length get() = sqrt(x * x + y * y + (z * z).toDouble()).toFloat()

    inline operator fun plus(other: Vec3f) = Vec3f(x + other.x, y + other.y, z + other.z)
    inline operator fun minus(other: Vec3f) = Vec3f(x - other.x, y - other.y, z - other.z)
    inline operator fun times(other: Vec3f) = Vec3f(x * other.x, y * other.y, z * other.z)
    inline operator fun times(other: Float) = Vec3f(x * other, y * other, z * other)
    inline operator fun div(other: Vec3f) = Vec3f(x / other.x, y / other.y, z / other.z)
    inline operator fun div(float: Float) = Vec3f(x / float, y / float, z / float)
    inline fun normalize() = if (length == 0f) zero() else this / length
    inline fun dot(other: Vec3f) = x * other.x + y * other.y + z * other.z


    inline fun selfAdd(other: Vec3f) = set(x + other.x, y + other.y, z + other.z)
    inline fun selfSubtract(other: Vec3f) = set(x - other.x, y - other.y, z - other.z)
    inline fun selfMultiply(other: Vec3f) = set(x * other.x, y * other.y, z * other.z)
    inline fun selfMultiply(other: Float) = set(x * other, y * other, z * other)
    inline fun selfDivide(other: Vec3f) = set(x / other.x, y / other.y, z / other.z)
    inline fun selfDivide(float: Float) = set(x / float, y / float, z / float)
    inline fun selfNormalize() = if (length == 0f) set(0f, 0f, 0f) else selfDivide(length)

    fun selfBlend(other: Vec3f, ratio: Float) {
        val inverseRation = 1f - ratio
        x = x * ratio + other.x * inverseRation
        y = y * ratio + other.y * inverseRation
        z = z * ratio + other.z * inverseRation
    }
}