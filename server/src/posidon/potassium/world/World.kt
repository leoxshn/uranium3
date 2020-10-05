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
                    player.sentChunks.removeIf { (it * Chunk.SIZE - player.position.toVec3i()).length > 200 }
                    val xx = (player.position.x / Chunk.SIZE).roundToInt()
                    val yy = (player.position.y / Chunk.SIZE).roundToInt()
                    val zz = (player.position.z / Chunk.SIZE).roundToInt()
                    for (x in -8..8) for (y in -8..8) for (z in -8..8) {
                        val chunkX = xx + x
                        val chunkY = yy + y
                        val chunkZ = zz + z
                        if (!player.sentChunks.contains(Vec3i(chunkX, chunkY, chunkZ)) && chunkY > -18 && chunkY < 18) {
                            player.send(ChunkPacket(chunks[chunkX, chunkY, chunkZ]
                                ?: generator.genChunk(chunkX, chunkY, chunkZ)
                                    .also { chunks[chunkX, chunkY, chunkZ] = it }))
                            player.sentChunks.add(Vec3i(chunkX, chunkY, chunkZ))
                        }
                    }
                }
                delta--
            }
            TimeUnit.NANOSECONDS.sleep((ns - (now - lastTime)).toLong())
        }
    }

    abstract fun tick()

    class ChunkMap : HashMap<Triple<Int, Int, Int>, Chunk>() {
        operator fun get(x: Int, y: Int, z: Int) = get(Triple(x, y, z))
        operator fun set(x: Int, y: Int, z: Int, chunk: Chunk) = set(Triple(x, y, z), chunk)
    }
}