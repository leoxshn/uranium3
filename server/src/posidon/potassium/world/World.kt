package posidon.potassium.world

import posidon.library.types.Vec3f
import posidon.potassium.net.Player
import posidon.potassium.running
import posidon.library.types.Vec3i
import posidon.potassium.Console
import posidon.potassium.content.Block
import posidon.potassium.world.gen.WorldGenerator
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.floor
import kotlin.math.roundToInt

abstract class World : Runnable {

    abstract val name: String
    protected abstract val generator: WorldGenerator

    private val chunks = HashMap<Vec3i, Chunk>()
    fun getChunk(chunkPos: Vec3i) = chunks.getOrPut(chunkPos) { generator.genChunk(chunkPos) }

    fun getBlock(position: Vec3i) = getBlock(position.x, position.y, position.z)
    fun getBlock(x: Int, y: Int, z: Int): Block? {
        val smallX = if (x % Chunk.SIZE < 0) Chunk.SIZE + x % Chunk.SIZE else x % Chunk.SIZE
        val smallY = if (y % Chunk.SIZE < 0) Chunk.SIZE + y % Chunk.SIZE else y % Chunk.SIZE
        val smallZ = if (z % Chunk.SIZE < 0) Chunk.SIZE + z % Chunk.SIZE else z % Chunk.SIZE
        val chunkPos = Vec3i(floor(x.toFloat() / Chunk.SIZE).toInt(), floor(y.toFloat() / Chunk.SIZE).toInt(), floor(z.toFloat() / Chunk.SIZE).toInt())
        return getChunk(chunkPos)[smallX, smallY, smallZ]
    }
    fun setBlock(position: Vec3i, block: Block?) = setBlock(position.x, position.y, position.z, block)
    fun setBlock(x: Int, y: Int, z: Int, block: Block?) {
        val smallX = if (x % Chunk.SIZE < 0) Chunk.SIZE + x % Chunk.SIZE else x % Chunk.SIZE
        val smallY = if (y % Chunk.SIZE < 0) Chunk.SIZE + y % Chunk.SIZE else y % Chunk.SIZE
        val smallZ = if (z % Chunk.SIZE < 0) Chunk.SIZE + z % Chunk.SIZE else z % Chunk.SIZE
        val chunkPos = Vec3i(floor(x.toFloat() / Chunk.SIZE).toInt(), floor(y.toFloat() / Chunk.SIZE).toInt(), floor(z.toFloat() / Chunk.SIZE).toInt())
        getChunk(chunkPos)[smallX, smallY, smallZ] = block
    }

    val players = ConcurrentLinkedQueue<Player>()

    private val deletionDistance = 400f
    internal val loadDistance = 240

    final override fun run() {
        var lastTime: Long = System.nanoTime()
        var delta = 0.0
        while (running) {
            val now: Long = System.nanoTime()
            delta += (now - lastTime) / 1000000000.0
            lastTime = now
            while (delta >= 0.01) {
                try {
                    val it = chunks.keys.iterator()
                    for (chunkPos in it) {
                        val r = (chunkPos * Chunk.SIZE)
                        var shouldDelete = true
                        for (player in players) {
                            if (r.apply { selfSubtract(player.position) }.length < deletionDistance) {
                                shouldDelete = false
                                break
                            } else {
                                player.sentChunks.remove(chunkPos)
                            }
                        }
                        if (shouldDelete) {
                            it.remove()
                        }
                    }
                } catch (e: OutOfMemoryError) {
                    generator.clearCache()
                    chunks.clear()
                    System.gc()
                    Console.beforeCmdLine {
                        Console.printProblem("OutOfMemoryError", " in world \"$name\"")
                        e.printStackTrace()
                    }
                }
                delta = 0.0
            }
        }
    }
}