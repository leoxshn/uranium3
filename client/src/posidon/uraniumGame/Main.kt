package posidon.uraniumGame

import posidon.uraniumGame.voxel.ChunkMap
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Camera
import posidon.uraniumGame.ui.loading.LoadingScreenComponent
import posidon.uraniumGame.net.Client
import posidon.uranium.gameLoop.EngineImplementation
import posidon.uranium.gameLoop.GameLoop
import posidon.uraniumGame.ui.loading.LoadingScreenScene

fun main(args: Array<String>) = GameLoop.loop(object : EngineImplementation {

    override fun init() {
        Renderer.camera = Camera("cam")

        val loading = LoadingScreenScene()
        Scene.set(loading)

        ChunkMap.init()

        BlockTextures.init(null)

        Client.start("localhost", 2512) {
            if (!it) Renderer.runOnThread {
                loading.component.setBackgroundPath("res/textures/ui/couldnt_connect.png")
            }
            else Renderer.runOnThread {
                Scene.set(World)
                loading.destroy()
                Renderer.camera!!.destroy()
                Renderer.camera = World.camera
            }
        }
    }

    override fun kill() {
        BlockTextures.clear()
        ChunkMap.destroy()
        Client.kill()
    }
})