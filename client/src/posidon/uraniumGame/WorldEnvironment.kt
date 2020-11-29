package posidon.uraniumGame

import posidon.library.types.Vec3f
import posidon.uranium.events.Event
import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.gameLoop.GameLoop
import posidon.uranium.nodes.environment.Environment
import posidon.uranium.nodes.environment.Sun
import kotlin.math.abs

class WorldEnvironment : Environment {

    companion object {
        private val SKY_NORMAL = Vec3f(0.4f, 0.58f, 0.72f)
        private val SKY_NIGHT = Vec3f(0f, 0.001f, 0.04f)
        private val AMBIENT_LIGHT_DAY = Vec3f(.88f, .885f, .89f)
        private val AMBIENT_LIGHT_NIGHT = Vec3f(0.09f, 0.137f, 0.180f)

        private const val MAX_TIME = 600.0
    }

    override val skyColor = Vec3f.zero()
    override val ambientLight = Vec3f.zero()

    override var sun: Sun? = Sun()

    private var time = MAX_TIME / 2.0
    var timeSpeed = 1

    override fun update(delta: Double) {
        time += timeSpeed * delta
        time %= MAX_TIME

        val a = (time - MAX_TIME / 2f) / MAX_TIME * 2
        val absA = abs(a.toFloat())

        skyColor.set(Vec3f.blend(SKY_NIGHT, SKY_NORMAL, absA))
        ambientLight.set(Vec3f.blend(AMBIENT_LIGHT_NIGHT, AMBIENT_LIGHT_DAY, absA))

        sun?.light?.set(Vec3f.blend(Sun.LIGHT_NIGHT, Sun.LIGHT_DAY, absA))
        sun?.setRotationDeg(a * 180.0)
    }

    override fun onEvent(event: Event) {
        if (event is PacketReceivedEvent) {
            when (event.tokens[0]) {
                "time" -> time = event.tokens[1].toDouble()
                "playerInfo" -> {
                    for (token in event.tokens) if (token.startsWith("time")) {
                        time = token.substring(6).toDouble()
                    }
                }
                "" -> GameLoop.end()
            }
        }
    }
}