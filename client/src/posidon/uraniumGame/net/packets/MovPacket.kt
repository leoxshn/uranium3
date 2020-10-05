package posidon.uraniumGame.net.packets

import posidon.library.types.Vec3f

class MovPacket(
    var position: Vec3f
) : Packet("mov") {
    override fun packToString() = "${position.x},${position.y},${position.z}"
}