package posidon.uranium.gameLoop

interface EngineImplementation {

    /**
     * Called when the engine is started, before the nodes start updating
     */
    fun init()

    /**
     * Called when the engine stops
     */
    fun kill()
}