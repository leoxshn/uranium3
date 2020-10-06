package posidon.library.types

import java.util.*
import kotlin.math.sqrt

data class Vec3i(var x: Int, var y: Int, var z: Int) {

    inline fun set(v: Vec3i) = set(v.x, v.y, v.z)
    inline fun set(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec3i) return false
        return other.x == x && other.y == y && other.z == z
    }

    override fun hashCode() = Objects.hash(x, y, z)
    override fun toString() = "vec3i($x, $y, $z)"
    inline fun toVec3f() = Vec3f(x.toFloat(), y.toFloat(), z.toFloat())

    companion object {
        inline fun zero() = Vec3i(0, 0, 0)
    }

    inline val length get() = sqrt(x * x + y * y + (z * z).toDouble()).toFloat()

    inline operator fun plus(other: Vec3i) = Vec3i(x + other.x, y + other.y, z + other.z)
    inline operator fun minus(other: Vec3i) = Vec3i(x - other.x, y - other.y, z - other.z)
    inline operator fun times(other: Vec3i) = Vec3i(x * other.x, y * other.y, z * other.z)
    inline operator fun times(other: Float) = Vec3f(x * other, y * other, z * other)
    inline operator fun times(other: Int) = Vec3i(x * other, y * other, z * other)
    inline operator fun div(other: Vec3i) = Vec3i(x / other.x, y / other.y, z / other.z)
    inline operator fun div(float: Float) = Vec3f(x / float, y / float, z / float)
    inline operator fun div(float: Int) = Vec3i(x / float, y / float, z / float)
    inline operator fun rem(b: Int) = Vec3i(x % b, y % b, z % b)
    inline fun normalize() = this / length
    inline fun dot(other: Vec3i) = x * other.x + y * other.y + z * other.z


    inline fun selfAdd(other: Vec3i) = set(x + other.x, y + other.y, z + other.z)
    inline fun selfSubtract(other: Vec3i) = set(x - other.x, y - other.y, z - other.z)
    inline fun selfMultiply(other: Vec3i) = set(x * other.x, y * other.y, z * other.z)
    inline fun selfMultiply(float: Float) = set((x * float).toInt(), (y * float).toInt(), (z * float).toInt())
    inline fun selfMultiply(int: Int) = set(x * int, y * int, z * int)
    inline fun selfDivide(other: Vec3i) = set(x / other.x, y / other.y, z / other.z)
    inline fun selfDivide(float: Float) = set((x / float).toInt(), (y / float).toInt(), (z / float).toInt())
    inline fun selfDivide(int: Int) = set(x / int, y / int, z / int)
    inline fun selfNormalize() = if (length == 0f) set(0, 0, 0) else selfDivide(length)
}