package io.posidon.uraniumGame.voxel

import io.posidon.uranium.graphics.Texture
import io.posidon.uranium.util.Heap
import io.posidon.uraniumPotassium.content.Block
import java.nio.IntBuffer

class Voxel(
    val uv: IntBuffer
) {
    inline val uvX get() = uv[0]
    inline val uvY get() = uv[1]

    companion object {

        const val SIZE_BYTES = Int.SIZE_BYTES * 2

        val dictionary = HashMap<Int, String>()

        private val blocks = HashMap<String, Voxel>().apply {
            Block.values().forEach {
                put(it.id, Voxel(Heap.int(it.uv.x, it.uv.y)))
            }
        }

        private val unknownBlock = Voxel(Heap.int(7, 7))

        operator fun get(id: String): Voxel = blocks[id] ?: unknownBlock

        private val albedo = Texture.load("client/res/textures/block/albedo.png")
        private val emission = Texture.load("client/res/textures/block/emission.png")
        private val specular = Texture.load("client/res/textures/block/specular.png")

        fun bindTileSet() {
            Texture.bind(albedo, emission, specular)
        }

        fun init() {
            albedo.create()
            emission.create()
            specular.create()
        }

        fun destroy() {
            albedo.destroy()
            emission.destroy()
            specular.destroy()
            blocks.forEach {
                Heap.free(it.value.uv)
            }
            Heap.free(unknownBlock.uv)
        }
    }
}