package posidon.uraniumGame.net

import posidon.uranium.gameLoop.GameLoop
import posidon.uraniumGame.events.PacketReceivedEvent
import posidon.uranium.nodes.Scene
import posidon.uraniumGame.World

object ReceivedPacketHandler {

    val blockDictionary = HashMap<Int, String>()

    operator fun invoke(packet: String) {
        val tokens = packet.split('&')
        Scene.passEvent(PacketReceivedEvent(System.currentTimeMillis(), packet, tokens))
        when (tokens[0]) {
            "time" -> World.environment.time = tokens[1].toDouble()
            "playerInfo" -> {
                for (token in tokens) if (token.startsWith("time")) {
                    World.environment.time = token.substring(6).toDouble()
                }
            }
            "" -> GameLoop.end()
        }
    }
}