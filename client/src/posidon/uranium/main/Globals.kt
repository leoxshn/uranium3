package posidon.uranium.main

import posidon.uranium.engine.Window
import posidon.uranium.engine.graphics.Renderer
import posidon.uranium.net.Client
import kotlin.system.exitProcess

object Globals {

    var running = true
    const val ns = 1000000000 / 60.0

    fun kill() {
        running = false
        Window.kill()
        Renderer.kill()
        Client.kill()
        exitProcess(0)
    }
}