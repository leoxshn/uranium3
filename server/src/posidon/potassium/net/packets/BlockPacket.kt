package posidon.potassium.net.packets

import posidon.library.types.Vec3i

class BlockPacket(
    var position: Vec3i,
    val id: Int
) : Packet("block") {
    override fun packToString() = "${position.x},${position.y},${position.z}&$id"
}