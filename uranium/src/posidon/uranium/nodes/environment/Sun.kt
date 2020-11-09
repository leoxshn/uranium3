package posidon.uranium.nodes.environment

import org.lwjgl.opengl.GL11
import posidon.library.types.Vec3f
import posidon.uranium.graphics.*
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.spatial.Eye
import kotlin.math.cos
import kotlin.math.sin

class Sun(name: String) : Node(name) {

    val normal = Vec3f(0f, 1f, 0f)

    var distance = 5f

    private var texture: Texture? = null
    private var rotationMatrix: Matrix4f? = null

    fun setTexturePath(path: String) = Renderer.runOnThread {
        texture?.destroy()
        texture = Texture(path)
    }

    override fun render(renderer: Renderer, eye: Eye) {
        shader.bind()
        shader["rotation"] = rotationMatrix ?: Matrix4f.identity()
        shader["view"] = eye.rotationMatrix
        shader["projection"] = Renderer.projectionMatrix
        shader["distance"] = distance

        Texture.bind(texture)

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_DST_ALPHA)

        Renderer.render(Mesh.QUAD)

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun destroy() {
        texture?.destroy()
    }

    inline fun setSunRotationDeg(deg: Double) = setSunRotationRadians(Math.toRadians(deg))
    fun setSunRotationRadians(rad: Double) {
        rotationMatrix = Matrix4f.rotateZ(rad)
        normal.run {
            x = -sin(rad).toFloat()
            y = cos(rad).toFloat()
        }
    }

    companion object {
        private val shader = Shader("/shaders/sunVertex.glsl", "/shaders/sunFragment.glsl")

        internal fun init() {
            shader.create()
        }

        internal fun destroy() {
            shader.destroy()
        }
    }
}