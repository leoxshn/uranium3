package posidon.uranium.nodes.ui.text

import posidon.library.types.Vec2f
import posidon.library.types.Vec3f
import posidon.uranium.graphics.Renderer
import posidon.uranium.nodes.spatial.Camera
import posidon.uranium.nodes.ui.UIComponent

class Text(
    name: String,
    font: MonospaceFont
) : UIComponent(name) {

    var font = font
        set(value) {
            field = value
            renderGlyphSize.set(value.glyphWidth.toFloat() / value.texture.width, value.glyphHeight.toFloat() / value.texture.height)
        }

    val textColor = Vec3f(1f, 1f, 1f)
    var string: String? = null
        set(value) {
            field = value
            uvs = font.getUVs(value ?: "")
        }

    private var uvs = arrayOf<Vec2f>()
    private val renderGlyphSize = Vec2f(font.glyphWidth.toFloat() / font.texture.width, font.glyphHeight.toFloat() / font.texture.height)

    override fun render(renderer: Renderer, camera: Camera) {
        super.render(renderer, camera)

        if (visible) {
            textShader.bind()
            font.bind()
            textShader["position"] = renderPosition
            textShader["size"] = renderSize * Vec2f(globalTransform.size.y.toFloat() / globalTransform.size.x, 1f)

            textShader["glyphSize"] = renderGlyphSize
            textShader["color"] = textColor
            textShader["text"] = uvs
            textShader["textLength"] = uvs.size
            Renderer.render(Renderer.QUAD_MESH)
        }
    }
}