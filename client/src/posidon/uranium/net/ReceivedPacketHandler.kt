package posidon.uranium.net

import posidon.uranium.engine.graphics.Renderer
import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.engine.input.events.PacketReceivedEvent
import posidon.uranium.engine.nodes.spatial.Camera
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.engine.nodes.spatial.Spatial
import posidon.uranium.main.Globals

object ReceivedPacketHandler {

    val blockDictionary = HashMap<Int, String>()

    operator fun invoke(packet: String) {
        val tokens = packet.split('&')
        RootNode.passEvent(PacketReceivedEvent(packet, tokens))
        when (tokens[0]) {
            "time" -> Globals.time = tokens[1].toDouble()
            "chunk" -> {
                Renderer.chunks.onEvent(PacketReceivedEvent(packet, tokens))
            }
            "playerInfo" -> {
                for (token in tokens) {
                    when {
                        token.startsWith("time") -> Globals.time = token.substring(6).toDouble()
                    }
                }
            }
            "" -> Globals.running = false
        }
    }
}