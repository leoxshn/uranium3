package posidon.uranium.gameLoop

import posidon.uranium.graphics.Window
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.NodeTree
import posidon.uranium.nodes.ui.UIComponent
import kotlin.concurrent.thread

object GameLoop {

    /**
     * The default [updateInterval] (in seconds)
     */
    const val DEFAULT_UPDATE_INTERVAL = 0.001

    /**
     * The time in seconds between each tree update
     */
    var updateInterval = DEFAULT_UPDATE_INTERVAL

    internal var running = true
        private set

    /**
     * Kind of the entry point of the engine.
     * To use the engine, call this function with your [EngineImplementation]
     */
    fun loop(implementation: EngineImplementation) {

        ////START/////////////////////////////////////
        Window.init(800, 600)
        Renderer.init()

        UIComponent.init()

        implementation.init()

        ////UPDATES///////////////////////////////////
        thread {
            var lastTime = System.nanoTime()
            var delta = 0.0
            while (running) {
                val now = System.nanoTime()
                delta += (now - lastTime) / 1000000000.0
                lastTime = now
                if (delta >= updateInterval) {
                    NodeTree.update(delta)
                    delta = 0.0
                }
            }
        }

        Renderer.loop()
        end()

        ////END///////////////////////////////////////
        implementation.kill()
        NodeTree.destroy()
        Renderer.kill()
        Window.kill()
    }

    /**
     * Stops the engine
     */
    fun end() {
        running = false
    }

    /**
     * Repeats [block] until the engine stops
     */
    fun loop(block: () -> Unit) {
        while (running) block()
    }
}