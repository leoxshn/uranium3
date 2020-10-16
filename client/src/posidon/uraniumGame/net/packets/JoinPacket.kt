package posidon.uraniumGame.net.packets

import posidon.uranium.net.Packet

class JoinPacket(
    var playerName: String,
    var id: String
) : Packet("join") {
    override fun packToString() = "$playerName&$id"
}