package posidon.uraniumGame.net.packets

import posidon.library.types.Vec3i
import posidon.uranium.net.Packet

class BlockBreakPacket(
    var position: Vec3i
) : Packet("blockbr") {
    override fun packToString() = "${position.x},${position.y},${position.z}"
}