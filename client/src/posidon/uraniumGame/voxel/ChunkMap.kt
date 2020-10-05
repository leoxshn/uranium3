package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.graphics.Window
import posidon.uraniumGame.BlockTextures
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.input.events.Event
import posidon.uraniumGame.events.PacketReceivedEvent
import posidon.uranium.nodes.Environment
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.spatial.Camera
import posidon.uraniumGame.net.ReceivedPacketHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class ChunkMap(name: String) : Node(name) {

    companion object {
        var blockShader = Shader("/shaders/blockVertex.glsl", "/shaders/blockFragment.glsl")

        fun init() {
            blockShader.create()
        }
    }

    val chunkLock = ReentrantLock()

    private val map = ConcurrentHashMap<Vec3i, Chunk>()

    operator fun get(x: Int, y: Int, z: Int) = map[Vec3i(x, y, z)]
    operator fun set(x: Int, y: Int, z: Int, chunk: Chunk) { map[Vec3i(x, y, z)] = chunk }
    operator fun get(v: Vec3i) = map[v]
    operator fun set(v: Vec3i, chunk: Chunk) { map[v] = chunk }

    override fun render(renderer: Renderer, camera: Camera) {
        blockShader.bind()
        blockShader["ambientLight"] = Environment.ambientLight
        blockShader["view"] = camera.viewMatrix
        BlockTextures.sheet.bind()
        for (chunk in map.values) {
            if (chunk.willBeRendered /*&& camera.isPositionInFov(chunk.position * Chunk.SIZE)*/) {
                blockShader["position"] = (chunk.position * Chunk.SIZE).toVec3f()
                Renderer.render(chunk.mesh!!)
            }
        }
    }

    override fun update(delta: Double) {
        Renderer.runOnMainThread {
            blockShader.bind()
            blockShader["skyColor"] = Environment.skyColor
            blockShader["projection"] = Window.projectionMatrix
        }

        chunkLock.lock()
        Chunk.chunksUpdating.removeIf {
            it.generateMeshAsync()
            true
        }
        map.keys.removeIf { chunkPos: Vec3i ->
            if ((chunkPos * Chunk.SIZE - Renderer.camera!!.position.toVec3i()).length > 500) {
                map[chunkPos]!!.clear()
                true
            } else false
        }
        chunkLock.unlock()
    }

    override fun onEvent(event: Event) {
        if (event is PacketReceivedEvent) {
            if (event.tokens[0] == "chunk") {
                val blocks = Compressor.decompressString(event.packet.substring(7 + event.tokens[1].length).newLineUnescape())
                val coords = event.tokens[1].substring(7).split(',')
                val chunkPos = Vec3i(coords[0].toInt(), coords[1].toInt(), coords[2].toInt())
                this[chunkPos] = blocks
            }
        }
    }

    operator fun set(chunkPos: Vec3i, blocks: String) {
        chunkLock.lock()

        val isNewChunk = map[chunkPos] == null
        if (isNewChunk) {
            map[chunkPos] = Chunk(chunkPos, this)
        }

        val chunk = map[chunkPos]!!

        for (i in 3..blocks.lastIndex step 4) {
            val material = (blocks[i - 3].toInt() shl 16) or blocks[i - 2].toInt()
            val posInChunk = Vec3i(
                (i / 4) / (Chunk.SIZE * Chunk.SIZE),
                (i / 4) / Chunk.SIZE % Chunk.SIZE,
                (i / 4) % Chunk.SIZE
            )
            chunk[posInChunk] =
                if (material == -1) null
                else Block(ReceivedPacketHandler.blockDictionary[material]!!, posInChunk, chunkPos, chunk)
        }

        Chunk.chunksUpdating.add(chunk)

        chunkLock.unlock()
    }

    /*operator fun set(posInChunk: Vec3i, chunkPos: Vec3i, id: String) {
        chunkLock.lock()
        if (chunks[chunkPos] == null) chunks[chunkPos] = Chunk(chunkPos, chunks)
        val cube = chunks[chunkPos]!![posInChunk]
        cube?.chunk?.blockBySides?.get(cube.sides)?.remove(cube)
        chunks[chunkPos]!![posInChunk] =
            if (id.isEmpty()) null
            else Block(id, posInChunk, chunkPos, chunk).also {
                /*runOnMainThread {
                    it.update()
                    if (!Chunk.chunksUpdating.contains(it.chunk)) {
                        Chunk.chunksUpdating.add(it.chunk)
                    }
                }*/
                blocksToUpdate.add(it)
            }
        chunkLock.unlock()
    }*/

    override fun destroy() {
        for (key in map.keys) {
            map[key]!!.clear()
            map.remove(key)
        }
    }
}