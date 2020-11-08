package posidon.uranium.nodes.environment

import posidon.uranium.graphics.Matrix4f
import posidon.library.types.Vec3f
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.graphics.Texture
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

        Renderer.render(Renderer.QUAD_MESH)
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