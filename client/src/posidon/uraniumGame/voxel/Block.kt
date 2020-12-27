package posidon.uraniumGame.voxel

import posidon.library.types.Vec2f
import posidon.uranium.graphics.Texture
import posidon.uranium.voxel.Voxel

class Block private constructor(
    private val uv: Vec2f
) : Voxel() {

    override fun getUV() = uv

    companion object {

        val dictionary = HashMap<Int, String>()

        private val blocks = hashMapOf(
            "dirt" to Block(Vec2f(0f, 0f)),
            "stone" to Block(Vec2f(1f, 0f)),
            "moonstone" to Block(Vec2f(2f, 0f)),
            "wood" to Block(Vec2f(0f, 1f)),
            "light_bricks" to Block(Vec2f(1f, 1f)),
            "moonstone_bricks" to Block(Vec2f(2f, 1f)),
            "slime" to Block(Vec2f(3f, 0f))
        )

        private val unknownBlock = Block(Vec2f(7f, 7f))

        operator fun get(id: String): Block = blocks[id] ?: unknownBlock

        private lateinit var albedo: Texture
        private lateinit var emission: Texture
        private lateinit var specular: Texture

        fun bindTileSet() {
            Texture.bind(albedo, emission, specular)
        }

        fun init() {
            albedo = Texture("client/res/textures/block/albedo.png")
            emission = Texture("client/res/textures/block/emission.png").apply {
                setMinFilter(Texture.Filter.NEAREST)
                setMagFilter(Texture.Filter.NEAREST)
            }
            specular = Texture("client/res/textures/block/specular.png")
        }

        fun destroy() {
            albedo.destroy()
            emission.destroy()
            specular.destroy()
        }
    }
}