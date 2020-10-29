package posidon.uranium.nodes.ui

import org.lwjgl.opengl.GL11
import posidon.library.types.Vec2f
import posidon.library.types.Vec2i
import posidon.library.types.Vec3f
import posidon.uranium.graphics.Window
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.graphics.Texture
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.spatial.Camera

abstract class UIComponent(
    name: String
) : Node(name) {

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
        const val WRAP_CONTENT = -1
    }

    val transform = Transform2D(
        Vec2i.zero(),
        Vec2i(MATCH_PARENT, MATCH_PARENT),
        Vec2f(1f, 1f),
        false)

    inline val position get() = transform.position
    inline val size get() = transform.size
    inline val scale get() = transform.scale

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
        if (parent is UIComponent) {
            val parentGlobalTransform = (parent as UIComponent).globalTransform
            globalTransform.position.set(parentGlobalTransform.position +
                (position.toVec2f() * parentGlobalTransform.scale).toVec2i())
            val parentSize = parentGlobalTransform.size
            if (transform.size.x == MATCH_PARENT && transform.size.y == MATCH_PARENT) {
                globalTransform.size.set(parentSize)
            } else {
                globalTransform.size.x = if (transform.size.x == MATCH_PARENT) {
                    parentSize.x
                } else transform.size.x
                globalTransform.size.y = if (transform.size.y == MATCH_PARENT) {
                    parentSize.y
                } else transform.size.y
            }
            globalTransform.scale.set(parentGlobalTransform.scale * scale)

            if (transform.keepAspectRatio) {
                val bg = background
                if (bg != null) {
                    if (parentSize.x / bg.width > parentSize.y / bg.height) {
                        globalTransform.size.x = globalTransform.size.x / bg.width * bg.height
                        globalTransform.size.y = globalTransform.size.y
                    } else {
                        globalTransform.size.x = globalTransform.size.x
                        globalTransform.size.y = globalTransform.size.y / bg.height * bg.width
                    }
                }
            }
        } else {
            globalTransform.position.set(transform.position)
            globalTransform.size.x = if (transform.size.x == MATCH_PARENT) {
                Window.width
            } else transform.size.x
            globalTransform.size.y = if (transform.size.y == MATCH_PARENT) {
                Window.height
            } else transform.size.y
            globalTransform.scale.set(transform.scale)

            if (transform.keepAspectRatio) {
                val bg = background
                if (bg != null) {
                    val w = Window.width.toFloat() / bg.width
                    val h = Window.height.toFloat() / bg.height
                    if (w > h) {
                        globalTransform.size.x = (globalTransform.size.x / w * h).toInt()
                        globalTransform.size.y = globalTransform.size.y
                    } else {
                        globalTransform.size.x = globalTransform.size.x
                        globalTransform.size.y = (globalTransform.size.y / h * w).toInt()
                    }
                }
            }
        }
    }

    internal val renderSize = Vec2f(1f, 1f)
    internal val renderPosition = Vec2f(0f, 0f)

    private fun updateRenderData() {
        renderPosition.set(
            globalTransform.position.x.toFloat() * 2 / Window.width,
            globalTransform.position.y.toFloat() * 2 / Window.height)
        renderSize.set(
            globalTransform.size.x.toFloat() / Window.width,
            globalTransform.size.y.toFloat() / Window.height)
    }

    var visible = true

    private var background: Texture? = null

    fun setBackgroundPath(path: String?) {
        background?.delete()
        background = Texture(path)
    }

    override fun render(renderer: Renderer, camera: Camera) {
        if (visible) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            val bg = background
            if (bg != null) {
                //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_SRC_ALPHA)
                shader.bind()
                bg.bind()
                shader["ambientLight"] = calculateLight()
                shader["position"] = renderPosition
                shader["size"] = renderSize
                Renderer.render(Renderer.QUAD_MESH)
            }
        }
    }

    protected val light = Vec3f(1f, 1f, 1f)
    protected open fun calculateLight() = light

    override fun destroy() {
        background?.delete()
    }
}