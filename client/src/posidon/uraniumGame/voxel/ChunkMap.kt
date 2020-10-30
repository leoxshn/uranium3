package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.events.Event
import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.voxel.VoxelChunkMap
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ChunkMap(name: String) : VoxelChunkMap<Chunk>(name) {

    override val chunkSize get() = Chunk.SIZE

    companion object {
        val blockDictionary = HashMap<Int, String>()
    }

    private val chunkLock = ReentrantLock()
    private val chunksUpdating = LinkedList<Chunk>()

    override fun preRender(shader: Shader) {
        Block.Textures.sheet.bind()
    }

    override fun update(delta: Double) {
        val eyePosition = Renderer.eye!!.position.toVec3i()
        chunkLock.lock()
        chunksUpdating.sortBy {
            (it.position * Chunk.SIZE).apply { selfSubtract(eyePosition) }.length
        }
        val it = chunksUpdating.iterator()
        while (it.hasNext()) {
            val chunk = it.next()
            val distance = chunk.absolutePosition.apply { selfSubtract(eyePosition) }.length
            if (distance > 500) {
                map.remove(chunk.position)
                it.remove()
            } else if (distance < 460 && chunk.allNeighboringChunksAreLoaded) {
                chunk.generateMeshAsync(min(max(abs(10 - (distance / 600f)).toInt(), 4), 8))
                it.remove()
            }
        }
        chunkLock.unlock()

        map.keys.removeIf { chunkPos: Vec3i ->
            if ((chunkPos * Chunk.SIZE).apply { selfSubtract(eyePosition) }.length > 500) {
                map.remove(chunkPos)!!.destroy()
                true
            } else false
        }
    }

    override fun onEvent(event: Event) {
        if (event is PacketReceivedEvent) {
            if (event.tokens[0] == "chunk") {
                val blocks = Compressor.decompressString(event.packet.substring(7 + event.tokens[1].length).newLineUnescape())
                val coords = event.tokens[1].substring(7).split(',')
                val chunkPos = Vec3i(coords[0].toInt(), coords[1].toInt(), coords[2].toInt())

                val chunk = map.getOrPut(chunkPos) {
                    Chunk(chunkPos, this)
                }

                var isEmpty = true
                for (i in 3..blocks.length step 4) {
                    val material = (blocks[i - 3].toInt() shl 16) or blocks[i - 2].toInt()
                    val posInChunk = Vec3i(
                        (i / 4) / (Chunk.SIZE * Chunk.SIZE),
                        (i / 4) / Chunk.SIZE % Chunk.SIZE,
                        (i / 4) % Chunk.SIZE
                    )
                    chunk.blocks[i / 4] =
                        if (material == -1) null
                        else Block(blockDictionary[material]!!, posInChunk, chunk).also { isEmpty = false }
                }

                if (!isEmpty) {
                    chunkLock.lock()
                    chunksUpdating.add(chunk)
                    chunkLock.unlock()
                }
            }
        }
    }

    override fun destroy() {
        for (key in map.keys) {
            map.remove(key)!!.destroy()
        }
    }
}