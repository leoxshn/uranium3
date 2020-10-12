package posidon.uraniumGame

import posidon.uraniumGame.voxel.ChunkMap
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.NodeTree
import posidon.uranium.nodes.spatial.Camera
import posidon.uraniumGame.ui.LoadingScreenComponent
import posidon.uraniumGame.net.Client
import posidon.uranium.gameLoop.EngineImplementation
import posidon.uranium.gameLoop.GameLoop

fun main(args: Array<String>) = GameLoop.loop(object : EngineImplementation {

    override fun init() {
        Renderer.camera = Camera("cam")

        val loading = LoadingScreenComponent("loadingScreen")
        NodeTree.setRoot(loading)

        ChunkMap.init()

        BlockTextures.init(null)

        Client.start("localhost", 2512) {
            if (!it) Renderer.runOnThread {
                loading.setBackgroundPath("res/textures/ui/couldnt_connect.png")
            }
            else Renderer.runOnThread {
                val world = World()
                NodeTree.setRoot(world)
                loading.destroy()
                Renderer.camera!!.destroy()
                Renderer.camera = world.camera
            }
        }
    }

    override fun kill() {
        BlockTextures.clear()
        ChunkMap.destroy()
        Client.kill()
    }
})