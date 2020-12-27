package posidon.potassium.world

import posidon.library.types.Vec3i
import posidon.potassium.Console
import posidon.potassium.content.Block
import posidon.potassium.net.Player
import posidon.potassium.running
import posidon.potassium.world.gen.WorldGenerator
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

abstract class World(
    val sizeInChunks: Int,
    val heightInChunks: Int
) : Runnable {

    inline val sizeInVoxels get() = sizeInChunks * Chunk.SIZE
    inline val heightInVoxels get() = heightInChunks * Chunk.SIZE

    abstract val name: String
    protected abstract val generator: WorldGenerator

    private val chunkLock = ReentrantLock()
    private val chunks = arrayOfNulls<Chunk>(sizeInChunks * sizeInChunks * heightInChunks)
    private fun getLoadedChunk(x: Int, y: Int, z: Int): Chunk? = chunks[x * sizeInChunks * heightInChunks + y * sizeInChunks + z].also { when {
        x < 0 || x >= sizeInChunks -> throw IllegalArgumentException("x = $x")
        z < 0 || z >= sizeInChunks -> throw IllegalArgumentException("z = $z")
        y < 0 || y >= heightInChunks -> throw IllegalArgumentException("y = $y")
    }}
    private fun setLoadedChunk(x: Int, y: Int, z: Int, chunk: Chunk?) { chunks[x * sizeInChunks * heightInChunks + y * sizeInChunks + z] = chunk }
    private inline fun getLoadedChunk(pos: Vec3i): Chunk? = getLoadedChunk(pos.x, pos.y, pos.z)
    fun getChunk(chunkPos: Vec3i): Chunk {
        chunkLock.lock()
        return getChunkUnsafe(chunkPos)
            .also { chunkLock.unlock() }
    }
    private inline fun getChunkUnsafe(chunkPos: Vec3i): Chunk {
        return getLoadedChunk(chunkPos) ?: generator.genChunk(chunkPos).also { chunks }
    }

    fun getBlock(position: Vec3i) = getBlock(position.x, position.y, position.z)
    fun getBlock(x: Int, y: Int, z: Int): Block? {
        val chunkPos = Vec3i(x / Chunk.SIZE, y / Chunk.SIZE, z / Chunk.SIZE)
        return getChunk(chunkPos)[x % Chunk.SIZE, y % Chunk.SIZE, z % Chunk.SIZE]
    }
    fun setBlock(position: Vec3i, block: Block?) = setBlock(position.x, position.y, position.z, block)
    fun setBlock(x: Int, y: Int, z: Int, block: Block?) {
        val chunkPos = Vec3i(x / Chunk.SIZE, y / Chunk.SIZE, z / Chunk.SIZE)
        getChunk(chunkPos)[x % Chunk.SIZE, y % Chunk.SIZE, z % Chunk.SIZE] = block
    }

    val players = ConcurrentLinkedQueue<Player>()

    private val deletionDistance = 400f
    private val loadDistance = 280f
    private val secPerTick = 2.0

    final override fun run() {
        var lastTime: Long = System.nanoTime()
        var delta = 0.0
        while (running) {
            val now: Long = System.nanoTime()
            delta += (now - lastTime) / 1000000000.0
            lastTime = now
            while (delta >= secPerTick) {
                chunkLock.lock()
                try {
                    for (x in 0 until sizeInChunks)
                        for (y in 0 until heightInChunks)
                            for (z in 0 until sizeInChunks)
                                if (getLoadedChunk(x, y, z) != null) {
                                    val r = Vec3i(x * Chunk.SIZE, y * Chunk.SIZE, z * Chunk.SIZE)
                                    var shouldDelete = true
                                    for (player in players) {
                                        if (r.apply { selfSubtract(player.position) }.length < deletionDistance) {
                                            shouldDelete = false
                                            break
                                        } else {
                                            player.sentChunks.remove(Vec3i(x, y, z))
                                        }
                                    }
                                    if (shouldDelete) {
                                        setLoadedChunk(x, y, z, null)
                                    }
                                }
                } catch (e: OutOfMemoryError) {
                    generator.clearCache()
                    System.gc()
                    Console.beforeCmdLine {
                        Console.printProblem("OutOfMemoryError", " in world \"$name\"")
                        e.printStackTrace()
                    }
                }
                chunkLock.unlock()
                delta -= secPerTick
            }
        }
    }

    fun sendChunks(player: Player) {
        chunkLock.lock()

        val loadChunks = (loadDistance / Chunk.SIZE).roundToInt()
        val xx = (player.position.x / Chunk.SIZE).roundToInt()
        val yy = (player.position.y / Chunk.SIZE).roundToInt()
        val zz = (player.position.z / Chunk.SIZE).roundToInt()
        val heightRange = min(max(yy-loadChunks, 0), heightInChunks - 1)..min(max(yy+loadChunks, 0), heightInChunks - 1)

        for (_x in xx - loadChunks..xx + loadChunks) {
            val x = run {
                val c = _x % sizeInChunks
                if (c < 0) sizeInChunks + c else c
            }
            for (_z in zz - loadChunks..zz + loadChunks) {
                val z = run {
                    val c = _z % sizeInChunks
                    if (c < 0) sizeInChunks + c else c
                }
                for (y in heightRange) {
                    val chunkPos = Vec3i(x, y, z)
                    if (!player.sentChunks.contains(chunkPos)) {
                        player.sendChunk(chunkPos, getChunkUnsafe(chunkPos))
                    }
                }
            }
        }

        chunkLock.unlock()
    }

    operator fun iterator() = chunks.iterator()
}