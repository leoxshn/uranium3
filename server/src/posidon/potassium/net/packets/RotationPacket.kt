package posidon.potassium.net.packets

class RotationPacket(
    var x: Float,
    var y: Float
) : Packet("rot") {
    override fun packToString() = "$x,$y"
}