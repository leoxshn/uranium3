package io.posidon.uranium.nodes.ui.text

import io.posidon.library.types.Vec2f
import io.posidon.library.types.Vec3f
import io.posidon.uranium.graphics.Mesh
import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.graphics.Window
import io.posidon.uranium.nodes.ui.View

class TextLine(
    window: Window,
    var font: MonospaceFont
) : View(window) {

    val color = Vec3f(1f, 1f, 1f)
    var string: String? = null
        set(value) {
            field = value
            val v = value ?: ""
            uvs = font.getUVs(v)
        }

    private var uvs = arrayOf<Vec2f>()
    private val textRenderSize: Vec2f = Vec2f.zero()

    override fun update(delta: Double) {
        super.update(delta)
        if (string == null) {
            textRenderSize.set(0f, 0f)
        } else {
            textRenderSize.set(getContentWidth().toFloat() / window.width, renderSize.y)
        }
    }

    override fun getContentWidth() = (string?.length ?: 0) * font.glyphWidth * globalTransform.size.y / font.glyphHeight

    override fun postRender(renderer: Renderer) {
        textShader.bind()
        font.bind()
        textShader["position"] = renderPosition
        textShader["size"] = textRenderSize

        textShader["glyphSize"] = font.renderGlyphSize
        textShader["color"] = color
        textShader["text"] = uvs
        textShader["textLength"] = uvs.size
        renderer.render(Mesh.QUAD)
    }
}