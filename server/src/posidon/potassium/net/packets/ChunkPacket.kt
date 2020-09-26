package posidon.potassium.net.packets

import posidon.library.util.Compressor
import posidon.library.util.newLineEscape
import posidon.potassium.world.Chunk
import java.lang.StringBuilder

class ChunkPacket(private val chunk: Chunk) : Packet("chunk") {

    override fun packToString(): String {
        val stringBuilder = StringBuilder()
        for (block in chunk) {
            if (block == null) stringBuilder
                .append((-1).toChar())
                .append((-1).toChar())
                .append((-1).toChar())
                .append((-1).toChar())
            else {
                stringBuilder
                    .append((block.material.ordinal ushr 16).toChar())
                    .append((block.material.ordinal).toChar())
                    .append((block.shape.ordinal ushr 16).toChar())
                    .append(block.shape.ordinal.toByte().toChar())
            }
        }
        return ("coords:" + chunk.chunkX + ',' + chunk.chunkY + ',' + chunk.chunkZ + '&' + Compressor.compressString(stringBuilder.toString()).newLineEscape())
    }
}