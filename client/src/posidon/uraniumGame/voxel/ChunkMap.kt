package posidon.uraniumGame.voxel

import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uraniumGame.BlockTextures
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.events.Event
import posidon.uraniumGame.events.PacketReceivedEvent
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Camera
import posidon.uraniumGame.net.ReceivedPacketHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class ChunkMap(name: String) : Node(name) {

    companion object {
        var blockShader = Shader("/shaders/blockVertex.glsl", /*"/shaders/wireframeGeometry.glsl",*/ "/shaders/blockFragment.glsl")

        fun init() {
            blockShader.create()
        }

        fun destroy() {
            blockShader.destroy()
        }
    }

    private val chunkLock = ReentrantLock()

    private val chunksUpdating = LinkedList<Chunk>()
    private val map = ConcurrentHashMap<Vec3i, Chunk>()

    operator fun get(x: Int, y: Int, z: Int) = map[Vec3i(x, y, z)]
    operator fun set(x: Int, y: Int, z: Int, chunk: Chunk) { map[Vec3i(x, y, z)] = chunk }
    operator fun get(v: Vec3i) = map[v]
    operator fun set(v: Vec3i, chunk: Chunk) { map[v] = chunk }

    override fun render(renderer: Renderer, camera: Camera) {
        blockShader.bind()
        blockShader["ambientLight"] = Scene.environment.ambientLight
        blockShader["view"] = camera.viewMatrix
        blockShader["skyColor"] = Scene.environment.skyColor
        blockShader["skyLight"] = Scene.environment.skyLight
        blockShader["sunNormal"] = Scene.environment.sunNormal
        blockShader["projection"] = Renderer.projectionMatrix
        BlockTextures.sheet.bind()
        for (chunk in map.values) {
            if (chunk.willBeRendered /*&& camera.isPositionInFov(chunk.position * Chunk.SIZE)*/) {
                blockShader["position"] = (chunk.position * Chunk.SIZE).toVec3f()
                Renderer.render(chunk.mesh!!)
            }
        }
    }

    override fun update(delta: Double) {
        val cameraPosition = Renderer.camera!!.position.toVec3i()
        chunkLock.lock()
        chunksUpdating.sortBy {
            (it.position * Chunk.SIZE).apply { selfSubtract(cameraPosition) }.length
        }
        val it = chunksUpdating.iterator()
        while (it.hasNext()) {
            val chunk = it.next()
            val distance = (chunk.position * Chunk.SIZE).apply { selfSubtract(cameraPosition) }.length
            if (distance > 500) {
                map.remove(chunk.position)
            } else {
                chunk.generateMeshAsync()
            }
            it.remove()
        }
        chunkLock.unlock()

        map.keys.removeIf { chunkPos: Vec3i ->
            if ((chunkPos * Chunk.SIZE).apply { selfSubtract(cameraPosition) }.length > 500) {
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
                this[chunkPos] = blocks
            }
        }
    }

    operator fun set(chunkPos: Vec3i, blocks: String) {

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
            chunk[posInChunk] =
                if (material == -1) null
                else Block(ReceivedPacketHandler.blockDictionary[material]!!, posInChunk, chunkPos, chunk).also { isEmpty = false }
        }

        if (!isEmpty) {
            chunkLock.lock()
            chunksUpdating.add(chunk)
            chunkLock.unlock()
        }
    }

    override fun destroy() {
        for (key in map.keys) {
            map.remove(key)!!.destroy()
        }
    }
}