package posidon.potassium.world

import posidon.library.types.Vec3i
import posidon.potassium.content.Block

class Chunk(val position: Vec3i) {

    private val blocks = arrayOfNulls<Block>(CUBE_SIZE)

    operator fun get(i: Int): Block? = blocks[i]
    operator fun get(x: Int, y: Int, z: Int): Block? = blocks[x * SIZE * SIZE + y * SIZE + z]
    operator fun set(i: Int, block: Block?) { blocks[i] = block }
    operator fun set(x: Int, y: Int, z: Int, block: Block?) {
        blocks[x * SIZE * SIZE + y * SIZE + z] = block
    }

    operator fun iterator() = blocks.iterator()

    companion object {
        const val SIZE = 64
        const val CUBE_SIZE = SIZE * SIZE * SIZE
    }
}