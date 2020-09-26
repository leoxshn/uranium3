package posidon.uranium.main

import posidon.uranium.net.Client
import posidon.uranium.engine.Window
import posidon.uranium.engine.graphics.Renderer
import posidon.uranium.engine.graphics.BlockTextures
import posidon.uranium.content.World
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.engine.nodes.spatial.Camera
import posidon.uranium.engine.nodes.spatial.voxel.ChunkMap
import posidon.uranium.engine.nodes.ui.LoadingScreenComponent
import posidon.uranium.engine.nodes.ui.UIComponent
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    ////START/////////////////////////////////////
    Window.init(800, 600)

    Renderer.init()

    ChunkMap.init()
    UIComponent.init()

    Renderer.camera = Camera("cam")

    val loading = LoadingScreenComponent("loadingScreen")
    RootNode.setScene(loading)

    BlockTextures.init(null)

    Client.start("localhost", 2512) {
        if (!it) Renderer.runOnMainThread {
            loading.setBackgroundPath("res/textures/ui/couldnt_connect.png")
        }
        else Renderer.runOnMainThread {
            val world = World()
            RootNode.setScene(world)
            loading.destroy()
            Renderer.camera!!.destroy()
            Renderer.camera = world.camera
        }
    }


    ////UPDATES///////////////////////////////////
    thread {
        var lastTime = System.nanoTime()
        var delta = 0.0
        while (Globals.running) {
            val now = System.nanoTime()
            delta += (now - lastTime) / Globals.ns
            lastTime = now
            if (delta >= 0.01) {
                RootNode.update(delta)
                delta = 0.0
            }
        }
    }


    ////RENDER////////////////////////////////////
    var lastTime = System.nanoTime()
    var delta = 0.0

    while (Window.isOpen && Globals.running) {
        val now = System.nanoTime()
        delta += (now - lastTime) / Globals.ns
        lastTime = now

        Window.update()
        Renderer.render()
        Window.swapBuffers()

        if (delta > 9) {
            Renderer.updateData()
            delta = 0.0
        }
    }


    ////END///////////////////////////////////////
    Globals.kill()
}