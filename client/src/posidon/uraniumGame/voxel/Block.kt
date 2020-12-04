package posidon.uraniumGame.voxel

import posidon.library.types.Vec2f
import posidon.library.types.Vec3i
import posidon.uranium.graphics.Texture
import posidon.uranium.voxel.Voxel
import java.util.HashMap

class Block private constructor(id: String, private val uv: Vec2f) : Voxel(id) {

    override fun getUV() = uv

    companion object {

        val dictionary = HashMap<Int, String>()

        private val uvs = hashMapOf(
            "dirt" to Vec2f(0f, 0f),
            "stone" to Vec2f(1f, 0f),
            "moonstone" to Vec2f(2f, 0f),
            "wood" to Vec2f(0f, 1f),
            "light_bricks" to Vec2f(1f, 1f),
            "moonstone_bricks" to Vec2f(2f, 1f),
            "slime" to Vec2f(3f, 0f)
        )

        private val blocks = uvs.mapValues { Block(it.key, it.value) }

        private val unknownBlock = Block("?", Vec2f(7f, 7f))

        operator fun get(id: String): Block = blocks[id] ?: unknownBlock

        private lateinit var albedo: Texture
        private lateinit var emission: Texture
        private lateinit var specular: Texture

        fun bindTileSet() {
            Texture.bind(albedo, emission, specular)
        }

        fun init() {
            albedo = Texture("client/res/textures/block/albedo.png")
            emission = Texture("client/res/textures/block/emission.png")
            specular = Texture("client/res/textures/block/specular.png")
        }

        fun destroy() {
            albedo.destroy()
            emission.destroy()
            specular.destroy()
        }
    }
}