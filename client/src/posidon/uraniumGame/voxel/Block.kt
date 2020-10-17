package posidon.uraniumGame.voxel

import posidon.library.types.Vec2f
import posidon.library.types.Vec3i
import posidon.uranium.graphics.Texture
import posidon.uranium.voxel.Voxel

class Block(
    id: String,
    posInChunk: Vec3i,
    chunk: Chunk
) : Voxel(id, posInChunk, chunk) {

    override fun getUV() = Textures.getUvForId(id)

    object Textures {

        fun getUvForId(id: String) = when (id) {
            "grass" -> Vec2f(0f, 0f)
            "stone" -> Vec2f(1f, 0f)
            "moonstone" -> Vec2f(2f, 0f)
            "wood" -> Vec2f(0f, 1f)
            "moonstone_bricks" -> Vec2f(2f, 1f)
            "slime" -> Vec2f(3f, 0f)
            else -> Vec2f(7f, 7f)
        }

        lateinit var sheet: Texture private set

        fun init() {
            sheet = Texture("res/textures/block/texture.png")
        }

        fun destroy() {
            sheet.delete()
        }
    }
}