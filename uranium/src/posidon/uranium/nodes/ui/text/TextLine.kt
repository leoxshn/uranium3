package posidon.uranium.nodes.ui.text

import posidon.library.types.Vec2f
import posidon.library.types.Vec3f
import posidon.uranium.graphics.Mesh
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Window
import posidon.uranium.nodes.ui.View

class TextLine(
    var font: MonospaceFont
) : View() {

    val color = Vec3f(1f, 1f, 1f)
    var string: String? = null
        set(value) {
            field = value
            val v = value ?: ""
            val u = font.getUVs(v)
            if (u == null) {
                Renderer.runOnThread {
                    uvs = font.getUVs(v)!!
                }
            }
            else uvs = u
        }

    private var uvs = arrayOf<Vec2f>()
    private val textRenderSize: Vec2f = Vec2f.zero()

    override fun update(delta: Double) {
        super.update(delta)

        if (string == null) {
            textRenderSize.set(0f, 0f)
        } else {
            textRenderSize.set(getContentWidth().toFloat() / Window.width, renderSize.y)
        }
    }

    override fun getContentWidth() = (string?.length ?: 0) * font.glyphWidth * globalTransform.size.y / font.glyphHeight

    override fun postRender() {
        textShader.bind()
        font.bind()
        textShader["position"] = renderPosition
        textShader["size"] = textRenderSize

        textShader["glyphSize"] = font.renderGlyphSize ?: Vec2f.zero()
        textShader["color"] = color
        textShader["text"] = uvs
        textShader["textLength"] = uvs.size
        Renderer.render(Mesh.QUAD)
    }
}