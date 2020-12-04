package posidon.potassium.net.packets

import posidon.library.util.Compressor
import posidon.library.util.newLineEscape
import posidon.potassium.world.Chunk
import java.lang.StringBuilder

class ChunkPacket(private val chunk: Chunk) : Packet("chunk") {

    override fun packToString(): String {
        val stringBuilder = StringBuilder()
        var nullCount = 0
        for (block in chunk) {
            if (block == null) nullCount++
            else {
                if (nullCount != 0) {
                    stringBuilder.append(
                        ((-1 ushr 16).toChar().toString() + (-1).toChar()).repeat(nullCount * 2))
                    nullCount = 0
                }
                stringBuilder
                    .append((block.material.ordinal ushr 16).toChar())
                    .append((block.material.ordinal).toChar())
                    .append((block.shape.ordinal ushr 16).toChar())
                    .append(block.shape.ordinal.toByte().toChar())
            }
        }
        return ("coords:" + chunk.position.x + ',' + chunk.position.y + ',' + chunk.position.z + '&' + Compressor.compressString(stringBuilder.toString(), Chunk.CUBE_SIZE * 8).newLineEscape())
    }
}