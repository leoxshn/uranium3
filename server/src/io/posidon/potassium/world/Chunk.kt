package io.posidon.potassium.world

import io.posidon.library.types.Vec3i
import io.posidon.library.util.Compressor
import io.posidon.uraniumPotassium.content.Block
import io.posidon.uraniumPotassium.content.Light
import io.posidon.uraniumPotassium.content.worldGen.Constants.CHUNK_SIZE
import io.posidon.uraniumPotassium.content.worldGen.Constants.CHUNK_SIZE_CUBE
import java.io.*
import java.util.*

class Chunk(
    private val blocks: Array<Block?> = arrayOfNulls(CHUNK_SIZE_CUBE)
) {

    val lightNodes = LinkedList<Light>()
    val lightLevels = ShortArray(CHUNK_SIZE_CUBE)

    operator fun get(i: Int): Block? = blocks[i]
    inline operator fun get(pos: Vec3i) = get(pos.x, pos.y, pos.z)
    inline operator fun get(x: Int, y: Int, z: Int): Block? = get(x * CHUNK_SIZE * CHUNK_SIZE + y * CHUNK_SIZE + z)
    operator fun set(i: Int, block: Block?) {
        blocks[i] = block
        lightNodes.removeIf {
            it.i == i
        }
        if (block != null && block.light != 0.toShort()) {
            lightNodes.add(Light(i, block.light))
        }
    }
    inline operator fun set(pos: Vec3i, block: Block?) = set(pos.x, pos.y, pos.z, block)
    inline operator fun set(x: Int, y: Int, z: Int, block: Block?) =
        set(x * CHUNK_SIZE * CHUNK_SIZE + y * CHUNK_SIZE + z, block)

    val indices get() = blocks.indices

    fun makePacketString(): String? {
        val stringBuilder = StringBuilder()
        var nullCount = 0
        for (i in blocks.indices) {
            val light = lightLevels[i]//.also { if (it != 0.toShort()) println(it) }
            val block = get(i)//?.let { if (light == 0.toShort()) Block.UNKNOWN else it }
            if (block == null) nullCount++
            else {
                if (nullCount != 0) {
                    for (j in 0 until nullCount) {
                        stringBuilder
                            .append((-1 ushr 16).toChar())
                            .append((-1).toChar())
                            .append(lightLevels[i - nullCount + j].toChar())
                    }
                    nullCount = 0
                }
                stringBuilder
                    .append((block.ordinal ushr 16).toChar())
                    .append((block.ordinal).toChar())
                    .append(light.toChar())
            }
        }
        return if (nullCount == CHUNK_SIZE_CUBE) null else stringBuilder.toString()
    }

    fun getSaveBytes(): ByteArray {
        val dictionary = HashMap<Int, String>()
        val (blocks, bl) = ByteArrayOutputStream().use {
            var nullCount = 0
            for (i in blocks.indices) {
                val block = get(i)
                if (block == null) nullCount++
                else {
                    if (nullCount != 0) {
                        for (j in 0 until nullCount) {
                            it.write(-1)
                        }
                        nullCount = 0
                    }
                    dictionary.putIfAbsent(block.ordinal, block.getSaveString())
                    it.write(block.ordinal)
                }
            }
            if (nullCount == CHUNK_SIZE_CUBE) {
                ByteArray(0) to 0
            } else {
                val b = it.toByteArray()
                b to b.size
            }
        }
        return if (bl != 0) {
            ByteArrayOutputStream().use {
                val db = ByteArrayOutputStream().use {
                    ObjectOutputStream(it).use { it.writeObject(dictionary) }
                    it.toByteArray()
                }
                it.write(db)
                it.write(blocks, 0, bl)
                val b = it.toByteArray()
                Compressor.compress(b, b.size).let { it.first.copyOf(it.second) }
            }
        } else blocks
    }

    companion object {
        fun readFromInputStream(inputStream: InputStream): Chunk? {
            if (inputStream.available() == 0) return null
            val (b, bl) = Compressor.decompress(inputStream.readAllBytes(), CHUNK_SIZE_CUBE * 4 + 128)
            val input = ByteArrayInputStream(b.copyOf(bl))
            val db = ObjectInputStream(input).use {
                it.readObject() as HashMap<Int, String>
            }
            val blockDictionary = HashMap<Int, Block>().apply {
                for ((i, s) in db) {
                    Block.values().find { it.id == s }?.let { put(i, it) }
                }
            }
            val blocks = Array(CHUNK_SIZE_CUBE) { _ ->
                val k = input.read()
                if (k == -1 || input.available() == 0) null
                else blockDictionary[k]
            }

            return Chunk(blocks).also {
                it.blocks.forEachIndexed { i, block ->
                    if (block != null && block.light != 0.toShort()) {
                        it.lightNodes.add(Light(i, block.light))
                    }
                }
            }
        }
    }
}

fun Chunk.clearLights() {
    lightLevels.fill(0)
    lightNodes.clear()
}