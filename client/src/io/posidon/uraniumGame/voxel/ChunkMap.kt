package io.posidon.uraniumGame.voxel

import io.posidon.library.types.Vec3f
import io.posidon.library.types.Vec3i
import io.posidon.library.util.Compressor
import io.posidon.library.util.newLineUnescape
import io.posidon.uranium.events.Event
import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.graphics.Shader
import io.posidon.uranium.nodes.Node
import io.posidon.uranium.nodes.spatial.BoundingBox
import io.posidon.uranium.nodes.spatial.Collider
import io.posidon.uranium.nodes.spatial.Eye
import io.posidon.uraniumGame.PacketReceivedEvent
import io.posidon.uraniumGame.Player
import io.posidon.uraniumPotassium.content.worldGen.Constants
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

class ChunkMap(val player: Player, val renderer: Renderer) : Node(), Collider {

    val checkLock = ReentrantLock()
    private val array = arrayOfNulls<Chunk>(sizeInChunks * sizeInChunks * heightInChunks)

    private val chunksUpdating = ConcurrentLinkedQueue<Chunk>()

    private var deletionDistance = 400f

    var doChunkProcessing = true

    private val chunkProcessingThread = thread (isDaemon = true, priority = 10) {
        while (doChunkProcessing) {
            val it = chunksUpdating.iterator()
            while (it.hasNext()) {
                val chunk = it.next()
                chunk.withNeighborsLoaded(this) {
                    it.remove()
                    chunk.generateMesh(this)
                    //println("loaded mesh of ${chunk.chunkX}, ${chunk.chunkY}, ${chunk.chunkZ}")
                }
            }
        }
    }

    private val chunkCleaningThread = thread (isDaemon = true) {
        while (doChunkProcessing) {
            val eyePosition = player.eye.position.toVec3i()
            for (x in 0 until sizeInChunks)
                for (y in 0 until heightInChunks)
                    for (z in 0 until sizeInChunks) {
                        val chunk = get(x, y, z)
                        if (chunk != null) {
                            checkLock.lock()
                            if (chunk.useCounter == 0) {
                                val distance = clipDistance(Vec3i(x * Constants.CHUNK_SIZE, y * Constants.CHUNK_SIZE, z * Constants.CHUNK_SIZE).apply { selfSubtract(eyePosition) }).length
                                if (distance > deletionDistance) {
                                    set(x, y, z, null)
                                    chunk.destroy(this)
                                }
                            }
                            checkLock.unlock()
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

                        val x = event.tokens[1].toInt()
                        val y = event.tokens[2].toInt()
                        val z = event.tokens[3].toInt()

                        var isChunkNew = false
                        val chunk = get(x, y, z) ?: run {
                            isChunkNew = true
                            Chunk(x, y, z)
                        }

                        val blocks = Compressor.decompressString(event.packet.substring(
                            9 + event.tokens[1].length + event.tokens[2].length + event.tokens[3].length
                        ).newLineUnescape(), Constants.CHUNK_SIZE_CUBE * 6)
                        var isEmpty = true
                        var i = 2
                        while (i < blocks.length) {
                            val material = (blocks[i - 2].toInt() shl 16) or blocks[i - 1].toInt()
                            val light = blocks[i].toShort()
                            chunk.setVoxel(i / 3,
                                if (material == -1) null
                                else Voxel[Voxel.dictionary[material]!!].also { isEmpty = false })
                            chunk.setLight(i / 3, light)
                            i += 3
                        }

                        if (isChunkNew)
                            set(x, y, z, chunk)
                        else for (j in i / 3 until Constants.CHUNK_SIZE_CUBE) {
                            chunk.setVoxel(j, null)
                            chunk.setLight(j, 0)
                        }
                        if (!isEmpty || isChunkNew) {
                            generateChunkMesh(chunk)
                            val endTime = System.currentTimeMillis()
                            //println("got chunk (duration: ${endTime - startTime}, pos: $x, $y, $z)")
                        } else {
                            chunk.deleteMesh(this)
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

    operator fun get(x: Int, y: Int, z: Int): Chunk? {
        when {
            x < 0 || x >= sizeInChunks -> throw IllegalArgumentException("x = $x")
            z < 0 || z >= sizeInChunks -> throw IllegalArgumentException("z = $z")
            y < 0 || y >= heightInChunks -> throw IllegalArgumentException("y = $y")
        }
        return array[x * sizeInChunks * heightInChunks + y * sizeInChunks + z]
    }
    operator fun set(x: Int, y: Int, z: Int, chunk: Chunk?) { array[x * sizeInChunks * heightInChunks + y * sizeInChunks + z] = chunk }

    override fun render(renderer: Renderer, eye: Eye) {
        renderer.enableDepthTest()

        blockShader.bind()
        blockShader["view"] = eye.viewMatrix
        blockShader["projection"] = eye.projectionMatrix

        Voxel.bindTileSet()

        for (_x in 0 until sizeInChunks) for (_z in 0 until sizeInChunks) for (_y in 0 until heightInChunks) {
            val chunk = get(_x, _y, _z)
            if (chunk != null && chunk.willBeRendered /*&& chunk.isInFov(eye)*/) {
                blockShader["position"] = run {
                    val absolutePosition = Vec3i(_x * Constants.CHUNK_SIZE, _y * Constants.CHUNK_SIZE, _z * Constants.CHUNK_SIZE)
                    val eyePos = eye.globalTransform.position.roundToVec3i()
                    var position = absolutePosition
                    for (x in -1..1) for (z in -1..1) {
                        val newPosition = Vec3i(x * sizeInVoxels, 0, z * sizeInVoxels).apply {
                            selfAdd(absolutePosition)
                        }
                        if ((newPosition - eyePos).length < (position - eyePos).length) position = newPosition
                    }
                    position
                }
                renderer.render(chunk.mesh!!)
            }
        }
    }

    fun getBlock(position: Vec3i) = getBlock(position.x, position.y, position.z)
    fun getBlock(x: Int, y: Int, z: Int): Voxel? =
        this[x / Constants.CHUNK_SIZE, y / Constants.CHUNK_SIZE, z / Constants.CHUNK_SIZE]
            ?.get(x % Constants.CHUNK_SIZE, y % Constants.CHUNK_SIZE, z % Constants.CHUNK_SIZE)
    fun setBlock(position: Vec3i, voxel: Voxel?) = setBlock(position.x, position.y, position.z, voxel)
    fun setBlock(x: Int, y: Int, z: Int, voxel: Voxel?): Chunk? =
        this[x / Constants.CHUNK_SIZE, y / Constants.CHUNK_SIZE, z / Constants.CHUNK_SIZE]?.apply {
            set(x % Constants.CHUNK_SIZE, y % Constants.CHUNK_SIZE, z % Constants.CHUNK_SIZE, voxel)
        }

    override fun collide(point: Vec3f): Boolean {
        return point.y != 0f && point.y != heightInChunks - 1f && getBlock(point.x.toInt(), point.y.toInt(), point.z.toInt()) != null
    }

    override fun collide(boundingBox: BoundingBox): Boolean {
        val origin = boundingBox.getRealOrigin()
        val o = origin.floorToVec3i()
        val e = (origin + boundingBox.size).floorToVec3i()

        for (x in o.x..e.x) for (y in o.y..e.y) for (z in o.z..e.z) {
            if (y >= 0 && y < heightInChunks * Constants.CHUNK_SIZE && getBlock(clipVoxelHorizontal(x), y, clipVoxelHorizontal(z)) != null) {
                return true
            }
        }

        return false
    }

    override fun destroy() {
        doChunkProcessing = false
        chunkProcessingThread.join()
        chunkCleaningThread.join()
        for (chunk in array) {
            chunk?.destroy(this)
        }
    }

    companion object {
        val sizeInChunks = 16
        val heightInChunks = 8
        val sizeInVoxels = sizeInChunks * Constants.CHUNK_SIZE
        val heightInVoxels = heightInChunks * Constants.CHUNK_SIZE

        inline fun clipChunkHorizontal(x: Int): Int {
            val r = x % sizeInChunks
            return if (r < 0) sizeInChunks + r else r
        }
        inline fun clipChunkHorizontal(x: Float): Float {
            val r = x % sizeInChunks
            return if (r < 0) sizeInChunks + r else r
        }
        inline fun clipVoxelHorizontal(x: Int): Int {
            val v = sizeInVoxels
            val r = x % v
            return if (r < 0) v + r else r
        }
        inline fun clipVoxelHorizontal(x: Float): Float {
            val v = sizeInVoxels
            val r = x % v
            return if (r < 0) v + r else r
        }

        inline fun clipDistance(distance: Vec3i): Vec3i {
            val xc = abs(distance.x) % sizeInVoxels
            val zc = abs(distance.z) % sizeInVoxels
            return Vec3i(min(xc, sizeInVoxels - xc), distance.y, min(zc, sizeInVoxels - zc))
        }

        private val blockShader = Shader("/shaders/blockVertex.glsl", "/shaders/blockFragment.glsl")

        internal fun init() {
            blockShader.create()
        }

        internal fun destroy() {
            blockShader.destroy()
        }
    }
}