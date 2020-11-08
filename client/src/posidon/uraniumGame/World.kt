package posidon.uraniumGame

import posidon.library.types.Vec3f
import posidon.library.types.Vec3i
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.environment.Sun
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

    var gravity = 50f

    val player = Player("player", this)

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

    val sun = Sun("sun").apply {
        setTexturePath("client/res/textures/environment/sun.png")
        environment.sun = this
    }

    init {
        add(sun)
        add(player)
        add(chunkMap)
        add(crossHair)
        add(chunkMeshThreadCounter)
        add(FpsIndicator("fps", Font()).apply {
            size.set(View.WRAP_CONTENT, 21)
        })
    }
}