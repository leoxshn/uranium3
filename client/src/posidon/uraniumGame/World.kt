package posidon.uraniumGame

import posidon.library.types.Vec3f
import posidon.library.types.Vec3i
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.ui.text.Text
import posidon.uraniumGame.ui.CrossHair
import posidon.uraniumGame.ui.Font
import posidon.uraniumGame.voxel.ChunkMap
import posidon.uraniumGame.voxel.Block
import posidon.uraniumGame.voxel.Chunk
import kotlin.math.floor

object World : Scene("World") {

    override val environment = WorldEnvironment()

    var gravity = 20f

    val camera = Player("camera", this)
    val chunkMap = ChunkMap("chunks")
    val crossHair = CrossHair("crosshair")
    //val hotBar = HotBarComponent("hotBar")

    init {
        add(camera)
        add(chunkMap)
        //add(hotBar)
        add(crossHair)
    }

    fun getBlock(position: Vec3f) = getBlock(floor(position.x).toInt(), floor(position.y).toInt(), floor(position.z).toInt())
    fun getBlock(position: Vec3i) = getBlock(position.x, position.y, position.z)
    fun getBlock(x: Int, y: Int, z: Int): Block? {
        val smallX = if (x % Chunk.SIZE < 0) Chunk.SIZE + x % Chunk.SIZE else x % Chunk.SIZE
        val smallY = if (y % Chunk.SIZE < 0) Chunk.SIZE + y % Chunk.SIZE else y % Chunk.SIZE
        val smallZ = if (z % Chunk.SIZE < 0) Chunk.SIZE + z % Chunk.SIZE else z % Chunk.SIZE
        val chunkPos = Vec3i(floor(x.toFloat() / Chunk.SIZE).toInt(), floor(y.toFloat() / Chunk.SIZE).toInt(), floor(z.toFloat() / Chunk.SIZE).toInt())
        return chunkMap[chunkPos]?.get(smallX, smallY, smallZ)
    }

    fun initOnRenderThread() {
        add(Text("text", Font()).apply {
            //setBackgroundPath("res/textures/ui/hotbar.png")
            string = "this is a test, pls work"
        })
    }
}