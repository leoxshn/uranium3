package io.posidon.uraniumGame

import io.posidon.uranium.Uranium
import io.posidon.uranium.graphics.Window
import io.posidon.uranium.net.client.Client
import io.posidon.uranium.nodes.Scene
import io.posidon.uranium.nodes.spatial.Eye
import io.posidon.uraniumGame.ui.LoadingScreenScene
import io.posidon.uraniumGame.voxel.ChunkMap
import io.posidon.uraniumGame.voxel.Voxel

val Client = Client("localhost", 2512)

fun main(args: Array<String>) {
    Uranium.init()
    val window = Window.new()
    window.renderer.eye = Eye(window.renderer)
    window.init(800, 600)
    Voxel.init()
    ChunkMap.init()

    val loading = LoadingScreenScene(window)
    Scene.set(loading)
    Client.onClose = {
        Scene.set(loading.apply {
            text.string = "There was a connection error"
        })
    }
    Client.onResult = {
        if (it) {
            Client.send("join&leoxshn&w04m58cyp49y59ti5ts9io3k".toCharArray())
            val line = Client.waitForPacket("init")
            val tokens = line.split('&')
            val defs = tokens[1].split(',')
            for (def in defs) {
                val eqI = def.indexOf('=')
                Voxel.dictionary[def.substring(0, eqI).toInt()] = def.substring(eqI + 1)
            }
            val x = tokens[2].toFloat()
            val y = tokens[3].toFloat()
            val z = tokens[4].toFloat()
            window.renderer.runOnThread {
                Scene.set(World(window).also {
                    it.player.position.set(x, y, z)
                })
                loading.destroy()
            }
        } else window.renderer.runOnThread {
            loading.text.string = "Couldn't connect :("
        }
    }
    Client.startAsync {
        val tokens = it.split('&')
        Scene.passEvent(PacketReceivedEvent(System.currentTimeMillis(), it, tokens))
    }

    window.loop()

    Client.stop()
    Uranium.destroy()
    ChunkMap.destroy()
    window.destroy()
    Voxel.destroy()
}