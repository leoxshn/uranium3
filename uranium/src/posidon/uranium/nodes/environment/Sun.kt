package posidon.uranium.nodes.environment

import posidon.library.types.Vec3f
import kotlin.math.cos
import kotlin.math.sin

class Sun(
    val light: Vec3f = Vec3f(1f, 1f, 1f),
    val normal: Vec3f = Vec3f(0f, 1f, 0f)
) {

    inline fun setRotationDeg(deg: Double) = setRotationRad(Math.toRadians(deg))
    inline fun setRotationRad(rad: Double) {
        normal.run {
            x = -sin(rad).toFloat()
            y = cos(rad).toFloat()
        }
    }

    companion object {
        val LIGHT_DAY = Vec3f(1.2f, 1.16f, 1.13f)
        val LIGHT_NIGHT = Vec3f(0f, 0f, 0f)
    }
}