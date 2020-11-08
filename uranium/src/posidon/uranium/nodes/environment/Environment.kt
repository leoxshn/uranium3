package posidon.uranium.nodes.environment

import posidon.library.types.Vec3f
import posidon.uranium.events.Event
import kotlin.math.cos
import kotlin.math.sin

interface Environment {

    val skyColor: Vec3f
    val skyLight: Vec3f
    val ambientLight: Vec3f

    var sun: Sun?

    fun update(delta: Double)
    fun onEvent(event: Event) {}
    fun destroy() {}
}