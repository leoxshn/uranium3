package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.events.Event
import posidon.uranium.events.MouseButtonPressedEvent
import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.graphics.Window
import posidon.uranium.input.Button
import posidon.uranium.input.Input
import posidon.uranium.net.Client
import posidon.uranium.voxel.VoxelChunkMap
import posidon.uraniumGame.World
import posidon.uraniumGame.net.packets.BlockBreakPacket
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.pow

class ChunkMap(name: String) : VoxelChunkMap<Block, Chunk>(name) {

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

        run {
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

        run {
            chunksUpdatingLock.lock()
            val it = chunksUpdating.iterator()
            while (it.hasNext()) {
                val chunk = it.next()
                val distance = chunk.absolutePosition.apply { selfSubtract(eyePosition) }.length
                if (distance < loadDistance && chunk.allNeighboringChunksAreLoaded) {
                    World.chunkMeshThreadCount++
                    chunk.generateMeshAsync(priority = ((1f - min(distance, loadDistance) / loadDistance).pow(2) * 3f + 7f).toInt()) {
                        World.chunkMeshThreadCount--
                    }
                    it.remove()
                } else if (distance > deletionDistance) {
                    map.remove(chunk.position)
                    it.remove()
                }
            }
            chunksUpdatingLock.unlock()
        }
    }

    override fun onEvent(event: Event) { thread (isDaemon = true) {
        when (event) {
            is PacketReceivedEvent -> {
                when (event.tokens[0]) {
                    "chunk" -> {
                        val startTime = System.currentTimeMillis()

                        val chunkPos = run {
                            val coords = event.tokens[1].split(',')
                            Vec3i(coords[0].toInt(), coords[1].toInt(), coords[2].toInt())
                        }

                        var isChunkNew = false
                        val chunk = map.getOrPut(chunkPos) {
                            isChunkNew = true
                            Chunk(chunkPos, this)
                        }

                        val blocks = Compressor.decompressString(event.packet.substring(7 + event.tokens[1].length).newLineUnescape(), Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 8)
                        var isEmpty = true
                        var i = 1
                        while (i <= blocks.length) {
                            val material = (blocks[i - 1].toInt() shl 16) or blocks[i].toInt()
                            chunk.blocks[i / 2] =
                                if (material == -1) null
                                else Block[Block.dictionary[material]!!].also { isEmpty = false }
                            i += 2
                        }

                        if (!isChunkNew) {
                            while (i < Chunk.SIZE * Chunk.SIZE * Chunk.SIZE * 2) {
                                chunk.blocks[i / 2] = null
                                i += 2
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
                    "block" -> {
                        val coords = event.tokens[1].split(',').let {
                            Vec3i(it[0].toInt(), it[1].toInt(), it[2].toInt())
                        }
                        setBlock(coords, null)?.generateMeshAsync(10)
                    }
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