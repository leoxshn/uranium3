package posidon.uranium.engine.nodes.spatial.voxel

import org.lwjgl.opengl.GL11
import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.engine.Window
import posidon.uranium.engine.graphics.BlockTextures
import posidon.uranium.engine.graphics.Renderer
import posidon.uranium.engine.graphics.Shader
import posidon.uranium.engine.input.events.Event
import posidon.uranium.engine.input.events.PacketReceivedEvent
import posidon.uranium.engine.nodes.Environment
import posidon.uranium.engine.nodes.Node
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.engine.nodes.spatial.Camera
import posidon.uranium.engine.nodes.spatial.Spatial
import posidon.uranium.net.ReceivedPacketHandler
import java.util.concurrent.ConcurrentHashMap

class ChunkMap(name: String) : Node(name) {

    companion object {
        var blockShader = Shader("/shaders/blockVertex.shade", "/shaders/blockFragment.shade")

        fun init() {
            blockShader.create()
        }
    }

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
        for (chunkPos in map.keys) {
            val chunk = map[chunkPos]!!
            if (chunk.willBeRendered && camera.isPositionInFov(chunkPos * Chunk.SIZE)) {
                blockShader["position"] = (chunk.position * Chunk.SIZE).toVec3f()
                chunk.mesh!!.bind()
                GL11.glDrawElements(GL11.GL_TRIANGLES, chunk.mesh!!.vertexCount, GL11.GL_UNSIGNED_INT, 0)
            }
        }
    }

    override fun update(delta: Double) {
        Renderer.runOnMainThread {
            blockShader.bind()
            blockShader["skyColor"] = Environment.skyColor
            blockShader["projection"] = Window.projectionMatrix
        }

        Renderer.chunkLock.lock()
        for (i in Renderer.blocksToUpdate.indices) {
            Renderer.blocksToUpdate.poll().run {
                update()
                if (!Chunk.chunksUpdating.contains(chunk)) {
                    Chunk.chunksUpdating.add(chunk)
                }
            }
        }
        Chunk.chunksUpdating.removeIf {
            it.startUpdateMesh()
            true
        }
        map.keys.removeIf { chunkPos: Vec3i ->
            if ((chunkPos * Chunk.SIZE - (RootNode["World/camera"] as Spatial).position.toVec3i()).length > 200) {
                map[chunkPos]!!.clear()
                true
            } else false
        }
        Renderer.chunkLock.unlock()
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
        Renderer.chunkLock.lock()

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
            val id = if (material == -1) "" else ReceivedPacketHandler.blockDictionary[material]!!
            if (isNewChunk) {
                val cube = chunk[posInChunk]
                cube?.chunk?.blockBySides?.get(cube.sides)?.remove(cube)
            }
            chunk[posInChunk] =
                    if (id.isEmpty()) null
                    else Block(id, posInChunk, chunkPos, chunk).also {
                        /*runOnMainThread {
                            it.update()
                            if (!Chunk.chunksUpdating.contains(it.chunk)) {
                                Chunk.chunksUpdating.add(it.chunk)
                            }
                        }*/
                        Renderer.blocksToUpdate.add(it)
                    }
        }

        Renderer.chunkLock.unlock()
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