package posidon.uraniumGame

import posidon.uranium.graphics.Filter
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.ui.FpsIndicator
import posidon.uranium.nodes.ui.Gravity
import posidon.uranium.nodes.ui.View
import posidon.uranium.nodes.ui.text.TextLine
import posidon.uranium.nodes.ui.CrossHair
import posidon.uraniumGame.ui.Font
import posidon.uraniumGame.voxel.ChunkMap

object World : Scene("World", WorldEnvironment()) {

    var gravity = 60f

    var chunkMeshThreadCount = 0
        set(value) {
            field = value
            chunkMeshThreadCounter.string = "chunkThreads: $value"
        }

    private val speedEffect = Filter("speedEffect", "/shaders/speedEffect.glsl", 1).apply {
        enabled = false
    }

    private val player = Player("player", this, speedEffect)

    private val chunkMeshThreadCounter = TextLine("node", Font()).apply {
        gravity = Gravity.TOP or Gravity.RIGHT
        string = "chunkThreads: _"
        size.set(View.WRAP_CONTENT, Font.SIZE)
    }

    init {
        add(player)
        add(ChunkMap("chunks"))
        add(Filter("filter", "/shaders/postprocessing.glsl", 4))
        add(speedEffect)
        add(CrossHair("crosshair").apply {
            setBackgroundPath("client/res/textures/ui/crosshair.png")
        })
        add(chunkMeshThreadCounter)
        add(FpsIndicator("fps", Font()).apply {
            size.set(View.WRAP_CONTENT, Font.SIZE)
        })
    }

    override fun onSet() {
        Renderer.eye!!.destroy()
        Renderer.eye = player.eye
    }
}