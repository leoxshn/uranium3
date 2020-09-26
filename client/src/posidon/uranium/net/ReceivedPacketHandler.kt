package posidon.uranium.net

import posidon.uranium.engine.input.events.PacketReceivedEvent
import posidon.uranium.engine.nodes.Environment
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.main.Globals

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
            "" -> Globals.running = false
        }
    }
}