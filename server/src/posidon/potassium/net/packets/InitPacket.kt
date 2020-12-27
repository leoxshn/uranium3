package posidon.potassium.net.packets

import posidon.potassium.content.Block

class InitPacket(
    val x: Float,
    val y: Float,
    val z: Float
) : Packet("init") {

    override fun packToString(): String {
        val strBuilder = StringBuilder()
        for (value in Block.values())
            strBuilder.append(value.ordinal).append('=').append(value.id).append(',')
        strBuilder.deleteCharAt(strBuilder.lastIndex)
        strBuilder.append("&$x&$y&$z")
        return strBuilder.toString()
    }
}