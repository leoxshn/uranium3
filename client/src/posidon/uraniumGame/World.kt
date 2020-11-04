package posidon.uraniumGame

import posidon.library.types.Vec3f
import posidon.library.types.Vec3i
import posidon.uranium.events.Event
import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.gameLoop.GameLoop
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.ui.FpsIndicator
import posidon.uranium.nodes.ui.Gravity
import posidon.uranium.nodes.ui.View
import posidon.uranium.nodes.ui.text.TextLine
import posidon.uraniumGame.ui.CrossHair
import posidon.uraniumGame.ui.Font
import posidon.uraniumGame.voxel.ChunkMap
import posidon.uraniumGame.voxel.Block
import posidon.uraniumGame.voxel.Chunk
import kotlin.math.floor

object World : Scene("World") {

    override val environment = WorldEnvironment()

    var gravity = 20f

    val eye = Player("eye", this)
    val chunkMap = ChunkMap("chunks")
    val crossHair = CrossHair("crosshair")
    val chunkMeshThreadCounter = TextLine("node", Font()).apply {
        gravity = Gravity.TOP or Gravity.RIGHT
        string = "chunkThreads: __"
        size.set(View.WRAP_CONTENT, 21)
    }
    var chunkMeshThreadCount = 0
        set(value) {
            field = value
            chunkMeshThreadCounter.string = "chunkThreads: $value"
        }

    init {
        add(eye)
        add(chunkMap)
        add(crossHair)
        add(chunkMeshThreadCounter)
        add(FpsIndicator("fps", Font()).apply {
            size.set(View.WRAP_CONTENT, 21)
        })
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
}