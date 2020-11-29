package posidon.uranium.nodes.ui.text

import posidon.library.types.Vec2f
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Texture

abstract class MonospaceFont(path: String) {

    abstract val glyphWidth: Int
    abstract val glyphHeight: Int
    abstract fun getPosition(char: Char): Vec2f
    abstract fun isFlipped(char: Char): Boolean

    internal var texture: Texture? = null
        private set

    internal var renderGlyphSize: Vec2f? = null

    init {
        Renderer.runOnThread {
            texture = Texture(path).apply {
                setMinFilter(Texture.Filter.SMOOTHER_NEAREST)
                setMagFilter(Texture.Filter.NEAREST)
            }
            renderGlyphSize = Vec2f(glyphWidth.toFloat() / texture!!.width, glyphHeight.toFloat() / texture!!.height)
        }
    }

    internal fun bind() {
        Texture.bind(texture)
    }

    internal fun getUVs(string: String) = if (texture == null) null else Array(string.length) {
        val char = string[it]
        val (x, y) = getPosition(char)
        Vec2f(x * glyphWidth / texture!!.width, y * glyphHeight / texture!!.height)
    }

    fun destroy() {
        texture?.destroy()
    }
}