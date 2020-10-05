package posidon.uraniumGame.net.packets

class JoinPacket(
    var playerName: String,
    var id: String
) : Packet("join") {
    override fun packToString() = "$playerName&$id"
}