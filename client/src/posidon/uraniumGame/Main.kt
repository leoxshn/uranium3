package posidon.uraniumGame

import posidon.uraniumGame.voxel.ChunkMap
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Camera
import posidon.uranium.net.Client
import posidon.uranium.gameLoop.EngineImplementation
import posidon.uranium.gameLoop.GameLoop
import posidon.uraniumGame.net.packets.JoinPacket
import posidon.uraniumGame.ui.loading.LoadingScreenScene

fun main(args: Array<String>) = GameLoop.loop(object : EngineImplementation {

    override fun init() {
        Renderer.camera = Camera("cam")

        val loading = LoadingScreenScene()
        Scene.set(loading)

        ChunkMap.init()

        BlockTextures.init(null)

        Client.start("localhost", 2512) {
            if (it) {
                Client.send(JoinPacket("leoxshn", "w04m58cyp49y59ti5ts9io3k"))

                val line = Client.waitForPacket("dict")
                val tokens = line.split('&')
                val defs = tokens[1].split(',')
                for (def in defs) {
                    val eqI = def.indexOf('=')
                    ChunkMap.blockDictionary[def.substring(0, eqI).toInt()] = def.substring(eqI + 1)
                }

                Renderer.runOnThread {
                    Scene.set(World)
                    loading.destroy()
                    Renderer.camera!!.destroy()
                    Renderer.camera = World.camera
                }
            } else Renderer.runOnThread {
                loading.component.setBackgroundPath("res/textures/ui/couldnt_connect.png")
            }
        }
    }

    override fun kill() {
        BlockTextures.clear()
        ChunkMap.destroy()
        Client.stop()
    }
})