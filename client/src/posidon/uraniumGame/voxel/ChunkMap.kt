package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.events.Event
import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.voxel.VoxelChunkMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class ChunkMap : VoxelChunkMap<Block, Chunk>() {

    override val chunkSize = 64

    private val chunksUpdating = ConcurrentLinkedQueue<Chunk>()

    private var deletionDistance = 400f

    override fun preRender(shader: Shader) {
        Block.bindTileSet()
    }

    var doChunkProcessing = true

    private val chunkProcessingThread = thread (isDaemon = true, priority = 10) {
        while (doChunkProcessing) {
            val it = chunksUpdating.iterator()
            while (it.hasNext()) {
                val chunk = it.next()
                chunk.withNeighborsLoaded {
                    chunk.generateMesh()
                    it.remove()
                    println("loaded mesh of ${chunk.position}")
                }
            }
        }
    }

    private val chunkCleaningThread = thread (isDaemon = true) {
        while (doChunkProcessing) {
            val eyePosition = Renderer.eye!!.position.toVec3i()
            for (x in 0 until sizeInChunks)
                for (y in 0 until heightInChunks)
                    for (z in 0 until sizeInChunks) {
                        val chunk = get(x, y, z)
                        if (chunk != null && chunk.useCounter == 0) {
                            val distance = Vec3i(x * chunkSize, y * chunkSize, z * chunkSize).apply { selfSubtract(eyePosition) }.length
                            if (distance > deletionDistance) {
                                set(x, y, z, null)
                                chunk.destroy()
                            }
                        }
                    }
        }
    }

    fun generateChunkMesh(chunk: Chunk) {
        chunksUpdating.add(chunk)
    }

    override fun onEvent(event: Event) { thread (isDaemon = true, priority = 10) {
        when (event) {
            is PacketReceivedEvent -> {
                when (event.tokens[0]) {
                    "chunk" -> {
                        val startTime = System.currentTimeMillis()

                        val chunkPos = run {
                            Vec3i(event.tokens[1].toInt(), event.tokens[2].toInt(), event.tokens[3].toInt())
                        }

                        var isChunkNew = false
                        val chunk = get(chunkPos) ?: run {
                            isChunkNew = true
                            Chunk(chunkPos, this).also { set(chunkPos, it) }
                        }

                        val blocks = Compressor.decompressString(event.packet.substring(
                            9 + event.tokens[1].length + event.tokens[2].length + event.tokens[3].length
                        ).newLineUnescape(), chunkSize * chunkSize * chunkSize * 4)
                        var isEmpty = true
                        var i = 1
                        while (i < blocks.length) {
                            val material = (blocks[i - 1].toInt() shl 16) or blocks[i].toInt()
                            chunk.blocks[i / 2] =
                                if (material == -1) null
                                else Block[Block.dictionary[material]!!].also { isEmpty = false }
                            i += 2
                        }

                        if (!isChunkNew) {
                            for (j in i / 2..chunkSize * chunkSize * chunkSize) {
                                chunk.blocks[j] = null
                            }
                        }
                        if (isChunkNew && !isEmpty) {
                            generateChunkMesh(chunk)
                            val endTime = System.currentTimeMillis()
                            println("got chunk (duration: ${endTime - startTime}, pos: $chunkPos)")
                        }
                    }
                    "block" -> {
                        val coords = event.tokens[1].split(',')
                        val x = coords[0].toInt()
                        val y = coords[1].toInt()
                        val z = coords[2].toInt()
                        setBlock(x, y, z, null)?.let { generateChunkMesh(it) }
                    }
                }
            }
        }
    }}

    override fun destroy() {
        doChunkProcessing = false
        chunkProcessingThread.join()
        chunkCleaningThread.join()
        super.destroy()
    }
}