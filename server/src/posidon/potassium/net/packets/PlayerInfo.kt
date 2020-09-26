package posidon.potassium.net.packets

class PlayerInfo(
    var x: Float,
    var y: Float,
    var z: Float,
    var movSpeed: Float,
    var jmpHeight: Float,
    var time: Float
) : Packet("playerInfo") {
    override fun packToString() = "coords:$x,$y,$z&movSpeed:$movSpeed&jmpHeight:$jmpHeight&time:$time"
}