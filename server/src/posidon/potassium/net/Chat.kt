package posidon.potassium.net

import posidon.potassium.net.packets.ChatPacket

object Chat {

    fun post(sender: String, message: String) {
        Server.sendToAllPlayers(ChatPacket(sender, message, false))
    }

    fun privateMessage(sender: String, message: String, receiver: Player) {
        receiver.send(ChatPacket(sender, message, true))
    }
}