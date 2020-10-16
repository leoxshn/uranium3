package posidon.potassium.world

object Worlds {

    private val worlds = HashMap<String, World>()

    operator fun get(name: String) = worlds[name]

    fun start(world: World) {
        worlds[world.name] = world
        Thread(world).start()
    }
}