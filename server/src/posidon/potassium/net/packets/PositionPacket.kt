package posidon.potassium.net.packets

class PositionPacket(
    var x: Float,
    var y: Float,
    var z: Float
) : Packet("pos") {
    override fun packToString() = "$x,$y,$z"
}