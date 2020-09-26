package posidon.library.types

import java.util.*
import kotlin.math.sqrt

data class Vec2f(var x: Float, var y: Float) {

    inline fun set(v: Vec2f) = set(v.x, v.y)
    inline fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec2f) return false
        return other.x.compareTo(x) == 0 && other.y.compareTo(y) == 0
    }

    override fun hashCode() = Objects.hash(x, y)
    override fun toString() = "vec2f($x, $y)"
    inline fun toVec2i() = Vec2i(x.toInt(), y.toInt())

    companion object {
        inline fun zero() = Vec2f(0f, 0f)
    }

    inline operator fun plus(other: Vec2f) =
        Vec2f(x + other.x, y + other.y)
    inline operator fun minus(other: Vec2f) =
        Vec2f(x - other.x, y - other.y)
    inline operator fun times(other: Vec2f) =
        Vec2f(x * other.x, y * other.y)
    inline operator fun times(other: Float) = Vec2f(x * other, y * other)
    inline operator fun div(other: Vec2f) =
        Vec2f(x / other.x, y / other.y)
    inline operator fun div(float: Float) = Vec2f(x / float, y / float)
    inline val length get() = sqrt(x * x + y * y)
    inline fun normalize() = if (length == 0f) Vec2f(0f, 0f) else this / length
    inline fun dot(other: Vec2f) = x * other.x + y * other.y
}