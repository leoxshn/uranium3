package posidon.potassium.net.packets

import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineEscape
import posidon.potassium.world.Chunk

class ChunkPacket(
    private val chunk: Chunk,
    private val chunkPos: Vec3i
) : Packet("chunk") {

    override fun packToString(): String {
        val stringBuilder = StringBuilder()
        var nullCount = 0
        for (block in chunk) {
            if (block == null) nullCount++
            else {
                if (nullCount != 0) {
                    stringBuilder.append(((-1 ushr 16).toChar().toString() + (-1).toChar()).repeat(nullCount))
                    nullCount = 0
                }
                stringBuilder
                    .append((block.ordinal ushr 16).toChar())
                    .append((block.ordinal).toChar())
            }
        }
        return (chunkPos.x.toString() + '&' + chunkPos.y + '&' + chunkPos.z + '&' + Compressor.compressString(stringBuilder.toString(), Chunk.CUBE_SIZE * 4).newLineEscape())
    }
}