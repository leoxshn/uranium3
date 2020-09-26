package posidon.uranium.main

import posidon.uranium.net.Client
import posidon.uranium.engine.Window
import posidon.uranium.engine.graphics.Renderer
import posidon.uranium.engine.graphics.BlockTextures
import posidon.uranium.content.World
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.engine.nodes.spatial.Camera
import posidon.uranium.engine.nodes.spatial.voxel.ChunkMap
import posidon.uranium.engine.nodes.ui.HotBar
import posidon.uranium.engine.nodes.ui.LoadingScreen
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    ////START/////////////////////////////////////
    Window.init(800, 600)
    Renderer.init()
    ChunkMap.init()
    val loadingScreen = LoadingScreen()
    Window.update()
    Renderer.renderUI()
    Window.swapBuffers()
    if (!Client.start("localhost", 2512)) {
        loadingScreen.setBackgroundPath("res/textures/ui/couldnt_connect.png")
        while (Window.isOpen) {
            Window.update()
            Renderer.renderUI()
            Window.swapBuffers()
        }
        Globals.kill()
    }
    BlockTextures.init(null)

    RootNode.setScene(World())

    thread {
        var lastTime = System.nanoTime()
        var delta = 0.0
        while (Globals.running) {
            val now = System.nanoTime()
            delta += (now - lastTime) / Globals.ns
            lastTime = now
            while (delta >= 1) {
                Globals.tick()
                delta--
            }
        }
    }

    thread {
        var lastTime = System.nanoTime()
        var delta = 0.0
        while (Globals.running) {
            val now = System.nanoTime()
            delta += (now - lastTime) / Globals.ns
            lastTime = now
            if (delta >= 0.01) {
                RootNode.update(delta)
                Renderer.chunks.update(delta)
                delta = 0.0
            }
        }
    }

    var lastTime = System.nanoTime()
    var delta = 0.0
    loadingScreen.visible = false
    ////GUI///////////////////////////////////////
    HotBar()
    ////LOOP//////////////////////////////////////
    val camera = RootNode["World/camera"] as Camera
    while (Window.isOpen && Globals.running) {
        val now = System.nanoTime()
        delta += (now - lastTime) / Globals.ns
        lastTime = now

        Window.update()
        Renderer.render(camera)
        Window.swapBuffers()

        if (delta > 9) {
            Renderer.updateData()
            delta = 0.0
        }
    }
    ////END///////////////////////////////////////
    Globals.kill()
}