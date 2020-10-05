package posidon.uraniumGame.net

import posidon.uranium.gameLoop.GameLoop
import posidon.uraniumGame.events.PacketReceivedEvent
import posidon.uranium.nodes.Environment
import posidon.uranium.nodes.RootNode

object ReceivedPacketHandler {

    val blockDictionary = HashMap<Int, String>()

    operator fun invoke(packet: String) {
        val tokens = packet.split('&')
        RootNode.passEvent(PacketReceivedEvent(packet, tokens))
        when (tokens[0]) {
            "time" -> Environment.time = tokens[1].toDouble()
            "playerInfo" -> {
                for (token in tokens) if (token.startsWith("time")) {
                    Environment.time = token.substring(6).toDouble()
                }
            }
            "" -> GameLoop.end()
        }
    }
}