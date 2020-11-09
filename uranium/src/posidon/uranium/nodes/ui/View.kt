package posidon.uranium.nodes.ui

import org.lwjgl.opengl.GL11
import posidon.library.types.Vec2f
import posidon.library.types.Vec2i
import posidon.library.types.Vec3f
import posidon.uranium.graphics.*
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.spatial.Eye

abstract class View(
    name: String
) : Node(name) {

    val transform = Transform2D(
        Vec2i.zero(),
        Vec2i(WRAP_CONTENT, WRAP_CONTENT),
        Vec2f(1f, 1f),
        false)

    inline val translation get() = transform.translation
    inline val size get() = transform.size
    inline val scale get() = transform.scale

    var gravity = Gravity.TOP or Gravity.LEFT

    val globalTransform = Transform2D(
        Vec2i.zero(),
        Vec2i.zero(),
        Vec2f(1f, 1f),
        false)

    override fun update(delta: Double) {
        updateGlobalTransform()
        updateRenderData()
    }

    private fun updateGlobalTransform() {
        val parentSize: Vec2i
        val parentScale: Vec2f

        if (parent is View) {
            val parentGlobalTransform = (parent as View).globalTransform
            parentSize = parentGlobalTransform.size
            parentScale = parentGlobalTransform.scale
            globalTransform.translation.set(parentGlobalTransform.translation +
                (translation.toVec2f() * parentGlobalTransform.scale).toVec2i())
        } else {
            parentSize = Vec2i(Window.width, Window.height)
            parentScale = Vec2f(1f, 1f)
            globalTransform.translation.set(transform.translation)
        }

        globalTransform.size.x = when (size.x) {
            MATCH_PARENT -> parentSize.x
            WRAP_CONTENT -> getContentWidth()
            else -> size.x
        }
        globalTransform.size.y = when (size.y) {
            MATCH_PARENT -> parentSize.y
            WRAP_CONTENT -> getContentHeight()
            else -> size.y
        }
        globalTransform.scale.set(parentScale.x * scale.x, parentScale.y * scale.y)

        if (transform.keepAspectRatio) {
            val bg = background
            if (bg != null) {
                if (parentSize.x / bg.width > parentSize.y / bg.height) {
                    globalTransform.size.x = globalTransform.size.x * bg.height / bg.width
                    globalTransform.size.y = globalTransform.size.y
                } else {
                    globalTransform.size.x = globalTransform.size.x
                    globalTransform.size.y = globalTransform.size.y * bg.width / bg.height
                }
            }
        }

        when {
            gravity and Gravity.CENTER_HORIZONTAL == Gravity.CENTER_HORIZONTAL -> {}
            gravity and Gravity.RIGHT == Gravity.RIGHT -> globalTransform.translation.x =
                globalTransform.translation.x + parentSize.x / 2 - globalTransform.size.x / 2
            gravity and Gravity.LEFT == Gravity.LEFT -> globalTransform.translation.x =
                globalTransform.translation.x - parentSize.x / 2 + globalTransform.size.x / 2
        }

        when {
            gravity and Gravity.CENTER_VERTICAL == Gravity.CENTER_VERTICAL -> {}
            gravity and Gravity.TOP == Gravity.TOP -> globalTransform.translation.y =
                globalTransform.translation.y + parentSize.y / 2 - globalTransform.size.y / 2
            gravity and Gravity.BOTTOM == Gravity.BOTTOM -> globalTransform.translation.y =
                globalTransform.translation.y - parentSize.y / 2 + globalTransform.size.y / 2
        }
    }

    internal val renderSize = Vec2f(1f, 1f)
    internal val renderPosition = Vec2f(0f, 0f)

    private fun updateRenderData() {
        renderPosition.set(
            globalTransform.translation.x.toFloat() * 2 / Window.width,
            globalTransform.translation.y.toFloat() * 2 / Window.height)
        renderSize.set(
            globalTransform.size.x.toFloat() / Window.width,
            globalTransform.size.y.toFloat() / Window.height)
    }

    var visible = true

    private var background: Texture? = null
    fun setBackgroundPath(path: String?) {
        background?.destroy()
        background = path?.let { Texture(it) }
    }

    protected val light = Vec3f(1f, 1f, 1f)
    protected open fun calculateLight() = light

    protected open fun getContentWidth(): Int {
        var maxWidth = 0
        for (child in children) {
            if (child is View && child.globalTransform.size.x > maxWidth) {
                maxWidth = child.globalTransform.size.x
            }
        }
        return maxWidth
    }
    protected open fun getContentHeight(): Int {
        var maxHeight = 0
        for (child in children) {
            if (child is View && child.globalTransform.size.y > maxHeight) {
                maxHeight = child.globalTransform.size.y
            }
        }
        return maxHeight
    }

    override fun render(renderer: Renderer, eye: Eye) {
        if (visible) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            val bg = background
            if (bg != null) {
                //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_SRC_ALPHA)
                shader.bind()
                Texture.bind(bg)
                shader["ambientLight"] = calculateLight()
                shader["position"] = renderPosition
                shader["size"] = renderSize
                Renderer.render(Mesh.QUAD)
            }
        }
    }

    override fun destroy() {
        background?.destroy()
    }

    companion object {
        var shader = Shader("/shaders/2DVertex.glsl", "/shaders/2DFragment.glsl")
        var textShader = Shader("/shaders/textVertex.glsl", "/shaders/textFragment.glsl")

        fun init() {
            shader.create()
            textShader.create()
        }

        fun destroy() {
            shader.destroy()
            textShader.destroy()
        }

        const val MATCH_PARENT = -1
        const val WRAP_CONTENT = -2
    }
}