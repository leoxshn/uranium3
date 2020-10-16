package posidon.potassium.world

import posidon.potassium.net.Player
import posidon.potassium.net.packets.ChunkPacket
import posidon.potassium.running
import posidon.library.types.Vec3i
import posidon.potassium.world.gen.WorldGenerator
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

abstract class World : Runnable {

    abstract val name: String
    protected abstract val generator: WorldGenerator
    protected abstract fun tick()

    protected val chunks = ChunkMap()

    val players = ConcurrentLinkedQueue<Player>()

    final override fun run() {
        var lastTime: Long = System.nanoTime()
        val amountOfTicks = 60.0
        val ns: Double = 1000000000.0 / amountOfTicks
        var delta = 0.0
        while (running) {
            val now: Long = System.nanoTime()
            delta += (now - lastTime) / ns
            lastTime = now
            while (delta >= 1) {
                tick()
                for (player in players) {
                    player.sentChunks.removeIf { (it * Chunk.SIZE).apply {
                        selfSubtract(player.position)
                    }.length > 400 }
                    val xx = (player.position.x / Chunk.SIZE).roundToInt()
                    val yy = (player.position.y / Chunk.SIZE).roundToInt()
                    val zz = (player.position.z / Chunk.SIZE).roundToInt()
                    for (x in -10..10) for (y in -10..10) for (z in -10..10) {
                        val chunkX = xx + x
                        val chunkY = yy + y
                        val chunkZ = zz + z
                        val chunkPos = Vec3i(chunkX, chunkY, chunkZ)
                        if (!player.sentChunks.contains(chunkPos) && chunkY > -18 && chunkY < 18) {
                            player.send(ChunkPacket(chunks[chunkPos]
                                ?: generator.genChunk(chunkX, chunkY, chunkZ)
                                    .also { chunks[chunkPos] = it }))
                            player.sentChunks.add(chunkPos)
                        }
                    }
                }
                delta--
            }
            TimeUnit.NANOSECONDS.sleep((ns - (now - lastTime)).toLong())
        }
    }

    class ChunkMap : HashMap<Vec3i, Chunk>() {
        operator fun get(x: Int, y: Int, z: Int) = get(Vec3i(x, y, z))
        operator fun set(x: Int, y: Int, z: Int, chunk: Chunk) = set(Vec3i(x, y, z), chunk)
    }
}