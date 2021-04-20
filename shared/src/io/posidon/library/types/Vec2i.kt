package io.posidon.library.types

import java.util.*
import kotlin.math.sqrt

data class Vec2i(var x: Int, var y: Int) {

    inline fun set(v: Vec2i) = set(v.x, v.y)
    inline fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec2i) return false
        return other.x.compareTo(x) == 0 && other.y.compareTo(y) == 0
    }

    override fun hashCode() = Objects.hash(x, y)
    override fun toString() = "vec2f($x, $y)"
    inline fun toVec2f() = Vec2f(x.toFloat(), y.toFloat())

    companion object {
        inline fun zero() = Vec2i(0, 0)
    }

    inline val length get() = sqrt(x.toFloat() * x + y * y)

    inline operator fun plus(other: Vec2i) = Vec2i(x + other.x, y + other.y)
    inline operator fun minus(other: Vec2i) = Vec2i(x - other.x, y - other.y)
    inline operator fun times(other: Vec2i) = Vec2i(x * other.x, y * other.y)
    inline operator fun times(other: Int) = Vec2i(x * other, y * other)
    inline operator fun div(other: Vec2i) = Vec2i(x / other.x, y / other.y)
    inline operator fun div(int: Int) = Vec2i(x / int, y / int)
    inline operator fun div(float: Float) = Vec2i((x / float).toInt(), (y / float).toInt())
    inline fun normalize() = if (length == 0f) Vec2i(0, 0) else this / length
    inline fun dot(other: Vec2i) = x * other.x + y * other.y


    inline fun selfAdd(other: Vec2i) = set(x + other.x, y + other.y)
    inline fun selfSubtract(other: Vec2i) = set(x - other.x, y - other.y)
    inline fun selfMultiply(other: Vec2i) = set(x * other.x, y * other.y)
    inline fun selfMultiply(other: Int) = set(x * other, y * other)
    inline fun selfDivide(other: Vec2i) = set(x / other.x, y / other.y)
    inline fun selfDivide(int: Int) = set(x / int, y / int)
    inline fun selfDivide(float: Float) = set((x / float).toInt(), (y / float).toInt())
    inline fun selfNormalize() = if (length == 0f) set(0, 0) else selfDivide(length)
}