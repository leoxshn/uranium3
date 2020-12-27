package posidon.potassium.world

import kotlin.concurrent.thread

object Worlds {

    private val worlds = HashMap<String, World>()

    operator fun get(name: String) = worlds[name]

    fun start(world: World) {
        worlds[world.name] = world
        thread(block = world::run)
    }
}