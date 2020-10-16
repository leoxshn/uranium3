package posidon.uranium.nodes.environment

import posidon.library.types.Vec3f
import posidon.uranium.events.Event
import kotlin.math.cos
import kotlin.math.sin

interface Environment {

    val skyColor: Vec3f
    val skyLight: Vec3f
    val ambientLight: Vec3f
    val sunNormal: Vec3f

    fun update(delta: Double)
    fun onEvent(event: Event) {}
    fun destroy() {}
}

inline fun Environment.setSunRotationDeg(deg: Double) = setSunRotationRadians(Math.toRadians(deg))
inline fun Environment.setSunRotationRadians(rad: Double) {
    sunNormal.run {
        x = -sin(rad).toFloat()
        y = cos(rad).toFloat()
    }
}