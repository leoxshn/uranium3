package io.posidon.library.util

import kotlin.random.Random

inline fun String.newLineEscape() = replace("\\", "\\\\").replace("\n", "\\n")
inline fun String.newLineUnescape() = replace(Regex("(?<=(?<!\\)\\(\\\\))*\\n"), "\n").replace("\\\\", "\\")

inline fun jumpLoop(topBound: Int, fn: (i: Int) -> Unit) {
    var i = 0
    var a = 0
    do {
        i += a
        a = 1 - a
        i *= -1
        fn(i)
    } while (i != topBound)
}

fun invSqrt(x: Float): Float {
    var x = x
    val xhalf = 0.5f * x
    var i = x.toBits()
    i = 0x5f3759df - (i shr 1)
    x = java.lang.Float.intBitsToFloat(i)
    x *= 1.5f - xhalf * x * x
    return x
}

fun invSqrt(x: Double): Double {
    var x = x
    val xhalf = 0.5 * x
    var i = x.toBits()
    i = 0x5fe6ec85e7de30daL - (i shr 1)
    x = java.lang.Double.longBitsToDouble(i)
    x *= 1.5 - xhalf * x * x
    return x
}

inline fun <T> Random.pickRandom(vararg args: T): T = args[nextInt(args.size)]