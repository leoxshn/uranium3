package posidon.uraniumGame

import posidon.uranium.gameLoop.EngineImplementation
import posidon.uranium.gameLoop.GameLoop
import posidon.uranium.graphics.Renderer
import posidon.uranium.net.Client
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Eye
import posidon.uraniumGame.net.packets.JoinPacket
import posidon.uraniumGame.ui.LoadingScreenScene
import posidon.uraniumGame.voxel.Block

fun main(args: Array<String>) = GameLoop.loop(object : EngineImplementation {

    override fun init() {
        Renderer.eye = Eye()

        val loading = LoadingScreenScene()
        Scene.set(loading)

        Block.init()

        Client.onClose = {
            Scene.set(loading.apply {
                text.string = "There was a connection error"
            })
        }

        Client.start("localhost", 2512) {
            if (it) {
                Client.send(JoinPacket("leoxshn", "w04m58cyp49y59ti5ts9io3k"))

                val line = Client.waitForPacket("init")
                val tokens = line.split('&')
                val defs = tokens[1].split(',')
                for (def in defs) {
                    val eqI = def.indexOf('=')
                    Block.dictionary[def.substring(0, eqI).toInt()] = def.substring(eqI + 1)
                }
                val x = tokens[2].toFloat()
                val y = tokens[3].toFloat()
                val z = tokens[4].toFloat()

                Renderer.runOnThread {
                    World.player.position.set(x, y, z)
                    Scene.set(World)
                    loading.destroy()
                }
            } else Renderer.runOnThread {
                loading.text.string = "Couldn't connect :("
            }
        }
    }

    override fun kill() {
        Block.destroy()
        Client.stop()
    }
})