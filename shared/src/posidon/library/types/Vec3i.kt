package posidon.library.types

import java.util.*
import kotlin.math.sqrt

data class Vec3i(var x: Int, var y: Int, var z: Int) {

    operator fun set(x: Int, y: Int, z: Int) {
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

    inline operator fun plus(other: Vec3i) =
        Vec3i(x + other.x, y + other.y, z + other.z)
    inline operator fun minus(other: Vec3i) =
        Vec3i(x - other.x, y - other.y, z - other.z)
    inline operator fun times(other: Vec3i) =
        Vec3i(x * other.x, y * other.y, z * other.z)
    inline operator fun times(other: Float) =
        Vec3f(x * other, y * other, z * other)
    inline operator fun times(other: Int) = Vec3i(x * other, y * other, z * other)
    inline operator fun div(other: Vec3i) =
        Vec3i(x / other.x, y / other.y, z / other.z)
    inline operator fun div(float: Float) =
        Vec3f(x / float, y / float, z / float)
    inline operator fun div(float: Int) = Vec3i(x / float, y / float, z / float)
    inline operator fun rem(b: Int) = Vec3i(x % b, y % b, z % b)
    inline val length get() = sqrt(x * x + y * y + (z * z).toDouble()).toFloat()
    inline fun normalize() = this / length
    inline fun dot(other: Vec3i) = x * other.x + y * other.y + z * other.z
}