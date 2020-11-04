package posidon.potassium.world

import posidon.potassium.net.Player
import posidon.potassium.net.packets.ChunkPacket
import posidon.potassium.running
import posidon.library.types.Vec3i
import posidon.potassium.Console
import posidon.potassium.world.gen.WorldGenerator
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

abstract class World : Runnable {

    abstract val name: String
    protected abstract val generator: WorldGenerator

    private val chunks = ChunkMap()

    val players = ConcurrentLinkedQueue<Player>()

    private val deletionDistance = 400
    private val loadDistance = 160

    final override fun run() {
        var lastTime: Long = System.nanoTime()
        var delta = 0.0
        while (running) {
            val now: Long = System.nanoTime()
            delta += (now - lastTime) / 1000000000.0
            lastTime = now
            while (delta >= 0.01) {
                try {
                    val loadChunks = loadDistance / Chunk.SIZE
                    val it = chunks.keys.iterator()
                    for (chunkPos in it) {
                        val r = (chunkPos * Chunk.SIZE)
                        var shouldDelete = true
                        for (player in players) {
                            if (r.apply {
                                selfSubtract(player.position)
                            }.length > deletionDistance) {
                                shouldDelete = false
                                player.sentChunks.remove(chunkPos)
                            }
                        }
                        if (shouldDelete) {
                            it.remove()
                        }
                    }
                    for (player in players) {
                        val xx = (player.position.x / Chunk.SIZE).roundToInt()
                        val yy = (player.position.y / Chunk.SIZE).roundToInt()
                        val zz = (player.position.z / Chunk.SIZE).roundToInt()
                        for (x in -loadChunks..loadChunks) for (y in -loadChunks..loadChunks) for (z in -loadChunks..loadChunks) {
                            val chunkPos = Vec3i(xx + x, yy + y, zz + z)
                            if (!player.sentChunks.contains(chunkPos) && chunkPos.y >= -7 && chunkPos.y <= 7) {
                                player.send(chunks[chunkPos] ?: generator.genChunk(chunkPos).also { chunks[chunkPos] = it })
                            }
                        }
                    }
                } catch (e: OutOfMemoryError) {
                    generator.clearCache()
                    chunks.clear()
                    System.gc()
                    Console.beforeCmdLine {
                        Console.printProblem("OutOfMemoryError", " in world \"$name\"")
                    }
                }
                delta = 0.0
            }
        }
    }

    class ChunkMap : HashMap<Vec3i, Chunk>() {
        operator fun get(x: Int, y: Int, z: Int) = get(Vec3i(x, y, z))
        operator fun set(x: Int, y: Int, z: Int, chunk: Chunk) = set(Vec3i(x, y, z), chunk)
    }
}