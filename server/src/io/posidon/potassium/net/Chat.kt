package io.posidon.potassium.net

import io.posidon.potassium.Server
import io.posidon.potassium.sendToAllPlayers
import io.posidon.uranium.net.server.ServerApi

object Chat {

    fun post(sender: String, message: String) {
        Server.sendToAllPlayers(ServerApi.chat(sender, message, false))
    }

    fun privateMessage(sender: String, message: String, receiver: Player) {
        receiver.send(ServerApi.chat(sender, message, true))
    }
}