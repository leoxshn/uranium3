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

    private val chunksUpdatingLock = ReentrantLock()
    private val chunksUpdating = LinkedList<Chunk>()

    private var deletionDistance = 400f
    private var loadDistance = 240f

    override fun preRender(shader: Shader) {
        Block.bindTileSet()
    }

    var doChunkProcessing = true
    val chunkProcessingThread = thread (isDaemon = true) {
        while (doChunkProcessing) processChunks()
    }

    private fun processChunks() {
        val eyePosition = Renderer.eye!!.position.toVec3i()
        val it = map.entries.iterator()
        while (it.hasNext()) {
            val (chunkPos, chunk) = it.next()
            val distance = (chunkPos * Chunk.SIZE).apply { selfSubtract(eyePosition) }.length
            if (distance > deletionDistance) {
                it.remove()
                chunk.destroy()
            } else {
                chunk.isCloseEnough = distance <= loadDistance
            }
        }
    }

    override fun update(delta: Double) {
        val eyePosition = Renderer.eye!!.position.toVec3i()

        chunksUpdatingLock.lock()
        val it = chunksUpdating.iterator()
        var i = 0
        while (i < 12 && it.hasNext()) {
            val chunk = it.next()
            val distance = chunk.absolutePosition.apply { selfSubtract(eyePosition) }.length
            if (distance < loadDistance && chunk.allNeighboringChunksAreLoaded) {
                World.chunkMeshThreadCount++
                chunk.generateMeshAsync(priority = ((1f - min(distance, loadDistance) / loadDistance).pow(2) * 5f + 5f).toInt()) {
                    World.chunkMeshThreadCount--
                }
                it.remove()
                i++
            } else if (distance > deletionDistance) {
                map.remove(chunk.position)
                it.remove()
                i++
            }
        }
        chunksUpdatingLock.unlock()
    }

    override fun onEvent(event: Event) { thread (isDaemon = true) {
        if (event is PacketReceivedEvent) {
            if (event.tokens[0] == "chunk") {
                val startTime = System.currentTimeMillis()

                val chunkPos = run {
                    val coords = event.tokens[1].substring(7).split(',')
                    Vec3i(coords[0].toInt(), coords[1].toInt(), coords[2].toInt())
                }

                var isChunkNew = false
                val chunk = map.getOrPut(chunkPos) {
                    isChunkNew = true
                    Chunk(chunkPos, this)
                }

                val blocks = Compressor.decompressString(event.packet.substring(7 + event.tokens[1].length).newLineUnescape(), Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 8)
                var isEmpty = true
                var i = 3
                while (i <= blocks.length) {
                    val material = (blocks[i - 3].toInt() shl 16) or blocks[i - 2].toInt()
                    /*val posInChunk = Vec3i(
                        (i / 4) / (Chunk.SIZE * Chunk.SIZE),
                        (i / 4) / Chunk.SIZE % Chunk.SIZE,
                        (i / 4) % Chunk.SIZE
                    )*/
                    chunk.blocks[i / 4] =
                        if (material == -1) null
                        else Block[Block.dictionary[material]!!].also { isEmpty = false }
                    i += 4
                }

                if (!isChunkNew) {
                    while (i < Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 4) {
                        chunk.blocks[i / 4] = null
                        i += 4
                    }
                }
                if (!isEmpty) {
                    chunksUpdatingLock.lock()
                    chunksUpdating.add(chunk)
                    chunksUpdatingLock.unlock()
                    val endTime = System.currentTimeMillis()
                    println("received chunk (duration = ${endTime - startTime})")
                }
            }
        }
    }}

    override fun destroy() {
        doChunkProcessing = false
        chunkProcessingThread.join()
        for (key in map.keys) {
            map.remove(key)!!.destroy()
        }
    }
}