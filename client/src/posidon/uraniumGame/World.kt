package posidon.uraniumGame

import posidon.library.types.Vec3f
import posidon.uranium.graphics.Filter
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.LineThing
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.ui.CrossHair
import posidon.uranium.nodes.ui.FpsIndicator
import posidon.uranium.nodes.ui.Gravity
import posidon.uranium.nodes.ui.View
import posidon.uranium.nodes.ui.text.TextLine
import posidon.uraniumGame.ui.Font
import posidon.uraniumGame.voxel.ChunkMap
import kotlin.math.roundToInt

object World : Scene(WorldEnvironment()) {

    var gravity = 60f

    private val speedEffect = Filter("/shaders/speedEffect.glsl", 1) { _, _ -> }.apply {
        enabled = false
    }

    val player = Player(this, speedEffect)

    private val xyzText = TextLine(Font()).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        string = "xyz: 0, 0, 0"
        translation.y = -Font.SIZE
        size.set(View.WRAP_CONTENT, Font.SIZE)
    }

    private val blockText = TextLine(Font()).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        string = "block: 0, 0, 0"
        translation.y = -Font.SIZE * 2
        size.set(View.WRAP_CONTENT, Font.SIZE)
    }

    private val chunkText = TextLine(Font()).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        string = "chunk: 0, 0, 0 (block: 0, 0, 0)"
        translation.y = -Font.SIZE * 3
        size.set(View.WRAP_CONTENT, Font.SIZE)
    }

    fun updateCoords() {
        val p = player.position
        xyzText.string = "xyz: ${p.x}, ${p.y}, ${p.z}"
        val x = p.x.roundToInt()
        val y = p.y.roundToInt()
        val z = p.z.roundToInt()
        blockText.string = "block: $x, $y, $z"
        chunkText.string = "chunk: ${x / chunkMap.chunkSize}, ${y / chunkMap.chunkSize}, ${z / chunkMap.chunkSize} (block: ${x % chunkMap.chunkSize}, ${y % chunkMap.chunkSize}, ${z % chunkMap.chunkSize})"
    }

    val chunkMap = ChunkMap()

    val selection = LineThing()

    init {
        add(player)
        add(chunkMap)
        add(Filter("/shaders/postprocessing.glsl", 4) { shader, eye ->
            shader["ambientLight"] = Scene.environment.ambientLight
            shader["skyColor"] = Scene.environment.skyColor
            shader["sunLight"] = Scene.environment.sun?.light ?: Vec3f.ZERO
            shader["sunNormal"] = Scene.environment.sun?.normal ?: Vec3f.ZERO
            shader["view"] = eye.viewMatrix
            shader["rotation"] = eye.rotationMatrix
            shader["projection"] = Renderer.projectionMatrix
        })
        add(speedEffect)
        add(selection)
        add(CrossHair().apply {
            setBackgroundPath("client/res/textures/ui/crosshair.png")
        })
        add(FpsIndicator(Font()).apply {
            size.set(View.WRAP_CONTENT, Font.SIZE)
        })
        add(xyzText)
        add(blockText)
        add(chunkText)
    }

    override fun onSet() {
        Renderer.eye!!.destroy()
        Renderer.eye = player.eye
    }
}