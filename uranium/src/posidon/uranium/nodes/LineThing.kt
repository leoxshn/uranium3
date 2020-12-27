package posidon.uranium.nodes

import posidon.uranium.graphics.Mesh
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.nodes.spatial.Eye
import posidon.uranium.nodes.spatial.Spatial

class LineThing : Spatial() {

    var vertices = floatArrayOf(
        -0.5f, 0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, 0.5f, -0.5f,
        -0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, 0.5f,
        0.5f, -0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        0.5f, 0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
        -0.5f, -0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, -0.5f,
        0.5f, 0.5f, -0.5f,
        0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, 0.5f,
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, 0.5f
    )

    val shader = Shader("/shaders/lineVertex.glsl", "/shaders/lineFragment.glsl").also {
        Renderer.runOnThread { it.create() }
    }

    lateinit var mesh: Mesh

    override fun render(renderer: Renderer, eye: Eye) {
        shader.bind()
        shader["projection"] = renderer.projectionMatrix
        shader["view"] = eye.viewMatrix
        shader["position"] = globalTransform.position
        val mesh = Mesh(intArrayOf(0, 1, 1, 0), Mesh.VBO(size = 3,
                1f, 1f, 0f,
                0f, 0f, 0f
        ))
        renderer.renderLines(mesh)
    }
}