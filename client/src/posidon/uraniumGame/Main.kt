package posidon.uraniumGame

import posidon.uraniumGame.voxel.ChunkMap
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.RootNode
import posidon.uranium.nodes.spatial.Camera
import posidon.uranium.nodes.ui.LoadingScreenComponent
import posidon.uraniumGame.net.Client
import posidon.uranium.gameLoop.EngineImplementation
import posidon.uranium.gameLoop.GameLoop

fun main(args: Array<String>) = GameLoop.loop(object : EngineImplementation {

    override fun init() {
        Renderer.camera = Camera("cam")

        val loading = LoadingScreenComponent("loadingScreen")
        RootNode.setScene(loading)

        ChunkMap.init()

        BlockTextures.init(null)

        Client.start("localhost", 2512) {
            if (!it) Renderer.runOnThread {
                loading.setBackgroundPath("res/textures/ui/couldnt_connect.png")
            }
            else Renderer.runOnThread {
                val world = World()
                RootNode.setScene(world)
                loading.destroy()
                Renderer.camera!!.destroy()
                Renderer.camera = world.camera
            }
        }
    }

    override fun kill() {
        BlockTextures.clear()
        ChunkMap.blockShader.destroy()
        Client.kill()
    }
})