package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.events.Event
import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.voxel.VoxelChunkMap
import posidon.uraniumGame.World
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ChunkMap(name: String) : VoxelChunkMap<Chunk>(name) {

    override val chunkSize get() = Chunk.SIZE

    companion object {
        val blockDictionary = HashMap<Int, String>()
    }

    private val chunksUpdatingLock = ReentrantLock()
    private val chunksUpdating = LinkedList<Chunk>()

    private var deletionDistance = 400
    private var loadDistance = 160

    override fun preRender(shader: Shader) {
        Block.Textures.bindTileSet()
    }

    override fun update(delta: Double) {
        val eyePosition = Renderer.eye!!.position.toVec3i()

        if (chunksUpdatingLock.tryLock()) {
            val it = chunksUpdating.iterator()
            while (it.hasNext()) {
                val chunk = it.next()
                val distance = chunk.absolutePosition.apply { selfSubtract(eyePosition) }.length
                if (distance > deletionDistance) {
                    map.remove(chunk.position)
                    it.remove()
                } else if (distance < loadDistance && chunk.allNeighboringChunksAreLoaded) {
                    World.chunkMeshThreadCount++
                    chunk.generateMeshAsync(min(max((10 - (distance / loadDistance) * 5f).toInt(), 5), 10)) {
                        World.chunkMeshThreadCount--
                    }
                    it.remove()
                }
            }
            chunksUpdatingLock.unlock()
        }

        run {
            val it = map.entries.iterator()
            var i = 0
            while (i < 24 && it.hasNext()) {
                val entry = it.next()
                val chunkPos = entry.key
                if ((chunkPos * Chunk.SIZE).apply { selfSubtract(eyePosition) }.length > deletionDistance) {
                    it.remove()
                    entry.value.destroy()
                }
                i++
            }
        }
    }

    override fun onEvent(event: Event) { thread (isDaemon = true) {
        if (event is PacketReceivedEvent) {
            if (event.tokens[0] == "chunk") {
                val blocks = Compressor.decompressString(event.packet.substring(7 + event.tokens[1].length).newLineUnescape(), Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 8)
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
                    chunksUpdatingLock.lock()
                    chunksUpdating.add(chunk)
                    chunksUpdatingLock.unlock()
                }
            }
        }
    }}

    override fun destroy() {
        for (key in map.keys) {
            map.remove(key)!!.destroy()
        }
    }
}