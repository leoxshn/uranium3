package posidon.potassium.world

import posidon.potassium.net.Player
import posidon.potassium.running
import posidon.library.types.Vec3i
import posidon.potassium.Console
import posidon.potassium.world.gen.WorldGenerator
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt

abstract class World : Runnable {

    abstract val name: String
    protected abstract val generator: WorldGenerator

    private val chunks = HashMap<Vec3i, Chunk>()
    fun getChunk(chunkPos: Vec3i) = chunks.getOrPut(chunkPos) { generator.genChunk(chunkPos) }

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