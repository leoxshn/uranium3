package posidon.potassium.net.packets

import posidon.potassium.content.Material
import java.lang.StringBuilder

class BlockDictionaryPacket : Packet("dict") {

    override fun packToString(): String {
        val strBuilder = StringBuilder()
        for (value in Material.values())
            strBuilder.append(value.ordinal).append('=').append(value.id).append(',')
        strBuilder.deleteCharAt(strBuilder.lastIndex)
        return strBuilder.toString()
    }
}