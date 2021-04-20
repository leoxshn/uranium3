package io.posidon.uraniumGame

import io.posidon.uranium.Uranium
import io.posidon.uranium.events.Event
import io.posidon.uranium.graphics.Filter
import io.posidon.uranium.graphics.Window
import io.posidon.uranium.nodes.Scene
import io.posidon.uranium.nodes.ui.CrossHair
import io.posidon.uranium.nodes.ui.FpsIndicator
import io.posidon.uranium.nodes.ui.Gravity
import io.posidon.uranium.nodes.ui.View
import io.posidon.uranium.nodes.ui.text.TextLine
import io.posidon.uraniumGame.ui.Font
import io.posidon.uraniumGame.voxel.ChunkMap
import io.posidon.uraniumPotassium.content.worldGen.Constants.CHUNK_SIZE
import kotlin.math.roundToInt

class World(val window: Window) : Scene() {

    val environment = WorldEnvironment()

    override fun update(delta: Double) {
        environment.update(delta)
    }

    override fun onEvent(event: Event) {
        environment.onEvent(event)
    }

    private val speedEffect = Filter("/shaders/speedEffect.glsl", 1, window.renderer) { _, _, _ -> }.apply {
        enabled = false
    }

    val player = Player(window.renderer, this, speedEffect)

    val font = Font().also {
        window.renderer.runOnThread {
            it.create()
        }
    }

    private val xyzText = TextLine(window, font).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        string = "xyz: 0, 0, 0"
        translation.y = -Font.SIZE
        size.set(View.WRAP_CONTENT, Font.SIZE)
    }

    private val blockText = TextLine(window, font).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        string = "block: 0, 0, 0"
        translation.y = -Font.SIZE * 2
        size.set(View.WRAP_CONTENT, Font.SIZE)
    }

    private val chunkText = TextLine(window, font).apply {
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
        chunkText.string = "chunk: ${x / CHUNK_SIZE}, ${y / CHUNK_SIZE}, ${z / CHUNK_SIZE} (block: ${x % CHUNK_SIZE}, ${y % CHUNK_SIZE}, ${z % CHUNK_SIZE})"
    }

    val chunkMap = ChunkMap(player, window.renderer)

    init {
        add(player)
        add(chunkMap)
        add(Filter("/shaders/postprocessing.glsl", 5, window.renderer) { _, shader, eye ->
            shader["time"] = Uranium.millis().toFloat()
            shader["view"] = eye.viewMatrix
            shader["rotation"] = eye.rotationMatrix
            shader["projection"] = eye.projectionMatrix
            shader["selection"] = player.currentBlockSelection
        })
        add(speedEffect)
        add(CrossHair(window).apply {
            setBackgroundPath("client/res/textures/ui/crosshair.png")
        })
        add(FpsIndicator(window, font).apply {
            size.set(View.WRAP_CONTENT, Font.SIZE)
        })
        add(xyzText)
        add(blockText)
        add(chunkText)
    }

    override fun onSet() {
        window.renderer.eye!!.destroy()
        window.renderer.eye = player.eye
    }
}