package io.posidon.potassium.world

import io.posidon.library.types.Vec3i
import io.posidon.potassium.Console
import io.posidon.potassium.net.Player
import io.posidon.potassium.net.Players
import io.posidon.potassium.running
import io.posidon.potassium.world.terrain.Terrain
import io.posidon.uranium.net.server.ServerApi
import io.posidon.uraniumPotassium.content.worldGen.Constants.CHUNK_SIZE
import kotlin.concurrent.thread
import kotlin.math.roundToInt

object World {

    val sizeInChunks: Int = 16
    val heightInChunks: Int = 8

    private lateinit var terrain: Terrain
    private lateinit var lightManager: LightManager
    private val saver = WorldSaver("world")

    fun init(seed: Long) {
        terrain = Terrain(seed, sizeInChunks, heightInChunks, saver)
        lightManager = LightManager(terrain)
        terrain.generateAndSaveAllChunks()
        thread {
            var lastTime: Long = System.nanoTime()
            var delta = 0.0
            while (running) {
                val now: Long = System.nanoTime()
                delta += (now - lastTime) / 1000000000.0
                lastTime = now
                while (delta >= secPerTick) {
                    terrain.withLock { unsafe ->
                        try {
                            for (x in 0 until sizeInChunks)
                                for (y in 0 until heightInChunks)
                                    for (z in 0 until sizeInChunks)
                                        if (unsafe.getLoadedChunk(x, y, z) != null) {
                                            val r = Vec3i(x * CHUNK_SIZE, y * CHUNK_SIZE, z * CHUNK_SIZE)
                                            var shouldDelete = true
                                            for (player in Players) {
                                                if (r.apply { selfSubtract(player.position) }.length < deletionDistance) {
                                                    shouldDelete = false
                                                    break
                                                } else {
                                                    player.sentChunks.remove(Vec3i(x, y, z))
                                                }
                                            }
                                            if (shouldDelete) {
                                                unsafe.setLoadedChunk(x, y, z, null)
                                            }
                                        }
                        } catch (e: OutOfMemoryError) {
                            System.gc()
                            Console.beforeCmdLine {
                                Console.printProblem("OutOfMemoryError", " in world")
                                e.printStackTrace()
                            }
                        }
                    }
                    delta -= secPerTick
                }
            }
        }
    }

    private val deletionDistance = 400f
    private val loadDistance = 280f
    private val secPerTick = 2.0

    fun sendChunks(player: Player) {
        //val chunksToSend = LinkedList<Pair<Vec3i, Chunk>>()
        var ss = 0.0
        var sc = 0
        val xx = (player.position.x / CHUNK_SIZE).roundToInt()
        val yy = (player.position.y / CHUNK_SIZE).roundToInt()
        val zz = (player.position.z / CHUNK_SIZE).roundToInt()
        val r = (loadDistance / CHUNK_SIZE).roundToInt()
        terrain.handleChunksIncrementally(xx, yy, zz, r) { accessor, chunkPos ->
            if (!player.sentChunks.contains(chunkPos)) {
                val s = System.nanoTime()
                val chunk = accessor.getChunkUnsafe(chunkPos)
                lightManager.updateChunkSpotLights(accessor, chunkPos.x, chunkPos.y, chunkPos.z)
                player.sendChunk(chunkPos, chunk)
                //chunksToSend.add(chunkPos to chunk)
                ss += System.nanoTime() - s
                sc++
                if (sc % 9 > 8) {
                    println("avg chunk time: ${ss/sc} ns")
                }
            }
        }
        //chunksToSend.forEach { player.sendChunk(it.first, it.second) }
    }

    fun getDefaultSpawnPosition(): Int {
        terrain.getChunk(0, 0, 0)
        return terrain.getHeight(0, 0) + 5
    }

    fun breakBlock(player: Player, x: Int, y: Int, z: Int) {
        terrain.setBlock(x, y, z, null)
        player.send(ServerApi.block(x, y, z, -1))
    }
}