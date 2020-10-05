package posidon.uranium.gameLoop

import posidon.uranium.graphics.Window
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.RootNode
import posidon.uranium.nodes.ui.UIComponent
import kotlin.concurrent.thread

object GameLoop {

    private const val ns = 1000000000 / 60.0

    const val DEFAULT_UPDATE_INTERVAL = 0.01

    var updateInterval = DEFAULT_UPDATE_INTERVAL
    var running = true
        private set

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
                delta += (now - lastTime) / ns
                lastTime = now
                if (delta >= updateInterval) {
                    RootNode.update(delta)
                    delta = 0.0
                }
            }
        }


        ////RENDER////////////////////////////////////
        var lastTime = System.nanoTime()
        var delta = 0.0

        while (Window.isOpen && running) {
            val now = System.nanoTime()
            delta += (now - lastTime) / ns
            lastTime = now

            Window.update()
            Renderer.renderObjects()
            Window.swapBuffers()

            if (delta > 9) {
                Renderer.updateData()
                delta = 0.0
            }
        }
        end()

        ////END///////////////////////////////////////
        implementation.kill()
        RootNode.destroy()
        Renderer.kill()
        Window.kill()
    }

    fun end() {
        running = false
    }
}