package posidon.uranium.nodes.ui.text

import posidon.library.types.Vec2f
import posidon.uranium.graphics.Texture

abstract class MonospaceFont(path: String) {

    abstract val glyphWidth: Int
    abstract val glyphHeight: Int
    abstract fun getPosition(char: Char): Vec2f
    abstract fun isFlipped(char: Char): Boolean

    internal val texture = Texture(path)

    internal fun bind() {
        texture.bind()
    }

    internal fun getUVs(string: String) = Array(string.length) {
        val char = string[it]
        val (x, y) = getPosition(char)
        Vec2f(x * glyphWidth / texture.width, y * glyphHeight / texture.height)
    }

    fun destroy() {
        texture.delete()
    }
}