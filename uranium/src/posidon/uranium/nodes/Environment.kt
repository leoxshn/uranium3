package posidon.uranium.nodes

import posidon.library.types.Vec3f
import posidon.uranium.graphics.Window
import kotlin.math.pow

object Environment : Node("environment") {

    private val SKY_NORMAL = Vec3f(0.4f, 0.65f, 0.956f)
    private val SKY_NIGHT = Vec3f(0f, 0.04f, 0.113f)
    private val LIGHT_DAY = Vec3f(1f, 1f, 1f)
    private val LIGHT_NIGHT = Vec3f(0.09f, 0.137f, 0.180f)
    private const val MAX_TIME = 24000

    var skyColor = Vec3f(0f, 0f, 0f)
    var ambientLight = skyColor
    var time = 0.0
    var timeSpeed = 1

    override fun update(delta: Double) {
        time += timeSpeed * delta
        time %= MAX_TIME

        skyColor = Vec3f.blend(SKY_NIGHT, SKY_NORMAL, ((time - MAX_TIME / 2f) / MAX_TIME * 2).pow(2.0).toFloat())
        ambientLight = Vec3f.blend(LIGHT_NIGHT, LIGHT_DAY, ((time - MAX_TIME / 2f) / MAX_TIME * 2).pow(2.0).toFloat())
        Window.backgroundColor.set(skyColor)
    }
}