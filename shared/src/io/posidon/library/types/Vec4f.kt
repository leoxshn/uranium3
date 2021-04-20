package io.posidon.library.types

import java.util.*
import kotlin.math.sqrt

data class Vec4f(var x: Float, var y: Float, var z: Float, var w: Float) {

    inline fun set(v: Vec4f) = set(v.x, v.y, v.z, v.w)
    inline fun set(x: Float, y: Float, z: Float, w: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vec4f) return false
        return other.x.compareTo(x) == 0 && other.y.compareTo(y) == 0 && other.z.compareTo(z) == 0 && other.w.compareTo(w) == 0
    }

    override fun hashCode() = Objects.hash(x, y, z, w)
    override fun toString() = "vec4f($x, $y, $z, $w)"

    companion object {
        inline fun zero() = Vec4f(0f, 0f, 0f, 0f)
        val ZERO = zero()
    }

    inline val length get() = sqrt(x * x + y * y + (z * z).toDouble()).toFloat()

    inline operator fun plus(other: Vec4f) = Vec4f(x + other.x, y + other.y, z + other.z, w + other.w)
    inline operator fun minus(other: Vec4f) = Vec4f(x - other.x, y - other.y, z - other.z, w - other.w)
    inline operator fun times(other: Vec4f) = Vec4f(x * other.x, y * other.y, z * other.z, w * other.w)
    inline operator fun times(other: Float) = Vec4f(x * other, y * other, z * other, w * other)
    inline operator fun div(other: Vec4f) = Vec4f(x / other.x, y / other.y, z / other.z, w / other.w)
    inline operator fun div(float: Float) = Vec4f(x / float, y / float, z / float, w / float)
    inline fun normalize() = if (length == 0f) zero() else this / length
    inline infix fun dot(other: Vec4f) = x * other.x + y * other.y + z * other.z + w * other.w


    inline fun selfAdd(other: Vec4f) = set(x + other.x, y + other.y, z + other.z, w + other.w)
    inline fun selfSubtract(other: Vec4f) = set(x - other.x, y - other.y, z - other.z, w - other.w)
    inline fun selfMultiply(other: Vec4f) = set(x * other.x, y * other.y, z * other.z, w * other.w)
    inline fun selfMultiply(other: Float) = set(x * other, y * other, z * other, w * other)
    inline fun selfDivide(other: Vec4f) = set(x / other.x, y / other.y, z / other.z, w / other.w)
    inline fun selfDivide(float: Float) = set(x / float, y / float, z / float, w / float)
    inline fun selfNormalize() = if (length == 0f) set(0f, 0f, 0f, 0f) else selfDivide(length)

    inline fun isNotZero() = x != 0f || y != 0f || z != 0f || w != 0f
}