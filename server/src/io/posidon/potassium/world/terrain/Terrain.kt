package io.posidon.potassium.world.terrain

import io.posidon.library.types.Vec3i
import io.posidon.potassium.Console
import io.posidon.potassium.world.Chunk
import io.posidon.potassium.world.World
import io.posidon.potassium.world.WorldSaver
import io.posidon.uraniumPotassium.content.Block
import io.posidon.uraniumPotassium.content.worldGen.Constants
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class Terrain(
    seed: Long,
    val sizeInChunks: Int,
    val heightInChunks: Int,
    val saver: WorldSaver
) {

    inline val sizeInVoxels get() = sizeInChunks * Constants.CHUNK_SIZE
    inline val heightInVoxels get() = heightInChunks * Constants.CHUNK_SIZE

    val chunks = arrayOfNulls<Chunk>(sizeInChunks * sizeInChunks * heightInChunks)

    val generator = WorldGenerator(seed, sizeInVoxels, heightInVoxels)

    fun getHeight(x: Int, z: Int): Int = generator.getHeight(x, z)

    private val chunkLock = ReentrantLock()

    private val unsafeChunkAccessor = UnsafeChunkAccessor(this)

    fun getBlock(x: Int, y: Int, z: Int): Block? {
        return getChunk(
            x / Constants.CHUNK_SIZE, y / Constants.CHUNK_SIZE, z / Constants.CHUNK_SIZE
        )[x % Constants.CHUNK_SIZE, y % Constants.CHUNK_SIZE, z % Constants.CHUNK_SIZE]
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Block?) {
        getChunk(
            x / Constants.CHUNK_SIZE, y / Constants.CHUNK_SIZE, z / Constants.CHUNK_SIZE
        )[x % Constants.CHUNK_SIZE, y % Constants.CHUNK_SIZE, z % Constants.CHUNK_SIZE] = block
    }

    fun getChunk(x: Int, y: Int, z: Int): Chunk {
        chunkLock.lock()
        return unsafeChunkAccessor.getChunkUnsafe(x, y, z).also { chunkLock.unlock() }
    }

    fun withLock(block: (UnsafeChunkAccessor) -> Unit) {
        chunkLock.lock()
        block(unsafeChunkAccessor)
        chunkLock.unlock()
    }

    fun generateAndSaveAllChunks() {
        val allChunks = sizeInChunks * sizeInChunks * heightInChunks
        var currentChunks = 0
        Console.beforeCmdLine {
            Console.println(Console.colors.BLUE_BRIGHT + "Loading chunks...")
        }
        val threads = LinkedList<Thread>()
        for (x in 0 until sizeInChunks) {
            threads.add(thread {
                for (z in 0 until sizeInChunks) {
                    for (y in 0 until heightInChunks) {
                        if (!saver.hasChunk(x, y, z)) {
                            saver.saveChunk(x, y, z, generator.genChunk(x, y, z))
                        }
                        currentChunks++
                    }
                }
                Console.beforeCmdLine {
                    Console.printInfo((currentChunks * 100 / (allChunks)).toString() + "% chunks loaded", " ($currentChunks out of $allChunks)")
                }
            })
        }
        threads.forEach { it.join() }
        Console.beforeCmdLine {
            Console.println(Console.colors.GREEN_BOLD_BRIGHT + "Done loading chunks!")
        }
    }

    fun handleChunksIncrementally(xx: Int, yy: Int, zz: Int, radius: Int, block: (UnsafeChunkAccessor, Vec3i) -> Unit) {
        val heightRange = (yy - radius)
            .coerceAtLeast(0)
            .coerceAtMost(World.heightInChunks - 1)..(yy + radius)
            .coerceAtLeast(0)
            .coerceAtMost(World.heightInChunks - 1)
        withLock { accessor ->
            for (y in heightRange) {
                block(accessor, Vec3i(xx, y, zz))
            }
            for (r in 1..radius) {
                val z0 = run {
                    val c = (zz - r) % World.sizeInChunks
                    if (c < 0) World.sizeInChunks + c else c
                }
                val z1 = run {
                    val c = (zz + r) % World.sizeInChunks
                    if (c < 0) World.sizeInChunks + c else c
                }
                for (_x in xx - r..xx + r) {
                    val x = run {
                        val c = _x % World.sizeInChunks
                        if (c < 0) World.sizeInChunks + c else c
                    }
                    for (y in heightRange) {
                        block(accessor, Vec3i(x, y, z0))
                        block(accessor, Vec3i(x, y, z1))
                    }
                }
                val x0 = run {
                    val c = (xx - r) % World.sizeInChunks
                    if (c < 0) World.sizeInChunks + c else c
                }
                val x1 = run {
                    val c = (xx + r) % World.sizeInChunks
                    if (c < 0) World.sizeInChunks + c else c
                }
                for (_z in zz - r + 1..zz - 1 + r) {
                    val z = run {
                        val c = _z % World.sizeInChunks
                        if (c < 0) World.sizeInChunks + c else c
                    }
                    for (y in heightRange) {
                        block(accessor, Vec3i(x0, y, z))
                        block(accessor, Vec3i(x1, y, z))
                    }
                }
            }
        }
    }

    class UnsafeChunkAccessor(val terrain: Terrain) {
        fun getLoadedChunk(x: Int, y: Int, z: Int): Chunk? {
            when {
                x < 0 || x >= World.sizeInChunks -> throw IllegalArgumentException("x = $x")
                z < 0 || z >= World.sizeInChunks -> throw IllegalArgumentException("z = $z")
                y < 0 || y >= World.heightInChunks -> throw IllegalArgumentException("y = $y")
            }
            return terrain.chunks[x * World.sizeInChunks * World.heightInChunks + y * World.sizeInChunks + z]
        }
        inline fun setLoadedChunk(x: Int, y: Int, z: Int, chunk: Chunk?) {
            terrain.chunks[x * World.sizeInChunks * World.heightInChunks + y * World.sizeInChunks + z] = chunk
        }
        inline fun getChunkUnsafe(chunkPos: Vec3i): Chunk = getChunkUnsafe(chunkPos.x, chunkPos.y, chunkPos.z)
        inline fun getChunkUnsafe(x: Int, y: Int, z: Int): Chunk {
            return getLoadedChunk(x, y, z) ?: terrain.saver.loadChunk(x, y, z) ?: genChunk(x, y, z)
        }
        inline fun genChunk(x: Int, y: Int, z: Int): Chunk {
            return terrain.generator.genChunk(x, y, z).also {
                setLoadedChunk(x, y, z, it)
                terrain.saver.saveChunk(x, y, z, it)
            }
        }
    }
}