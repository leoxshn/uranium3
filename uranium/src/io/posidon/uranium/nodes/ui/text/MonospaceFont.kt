package io.posidon.uranium.nodes.ui.text

import io.posidon.library.types.Vec2f
import io.posidon.uranium.graphics.Texture

abstract class MonospaceFont(path: String) {

    abstract val glyphWidth: Int
    abstract val glyphHeight: Int
    abstract fun getPosition(char: Char): Vec2f
    abstract fun isFlipped(char: Char): Boolean

    internal val texture: Texture = Texture.load(path)

    internal val renderGlyphSize: Vec2f by lazy {
        Vec2f(glyphWidth.toFloat() / texture.width, glyphHeight.toFloat() / texture.height)
    }

    fun create() {
        texture.run {
            create()
            setMinFilter(Texture.MinFilter.SMOOTHER_NEAREST)
            setMagFilter(Texture.MagFilter.NEAREST)
        }
    }

    internal fun bind() {
        Texture.bind(texture)
    }

    internal fun getUVs(string: String) = Array(string.length) {
        val char = string[it]
        val (x, y) = getPosition(char)
        Vec2f(x * glyphWidth / texture.width, y * glyphHeight / texture.height)
    }

    fun destroy() {
        texture.destroy()
    }
}