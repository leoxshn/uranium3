package posidon.uranium.nodes.environment

import posidon.library.types.Vec3f
import posidon.uranium.graphics.Mesh
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Eye

class Skybox(name: String) : Node(name) {

    override fun render(renderer: Renderer, eye: Eye) {
        shader.bind()

        shader["projection"] = Renderer.projectionMatrix
        shader["rotation"] = eye.rotationMatrix
        shader["skyColor"] = Scene.environment.skyColor
        shader["sunNormal"] = Scene.environment.sun?.normal ?: Vec3f.ZERO

        renderer.render(CUBE)
    }

    companion object {

        private val shader = Shader("/shaders/skyVertex.glsl", "/shaders/skyFragment.glsl")

        private val INDICES = intArrayOf(
            2, 1, 3, 3, 1, 0,       // Front
            11, 8, 9, 11, 10, 8,    // Top
            7, 12, 5, 7, 13, 12,    // Right
            14, 15, 6, 4, 14, 6,    // Left
            19, 16, 17, 19, 18, 16, // Bottom
            4, 6, 7, 5, 4, 7        // Back
        )

        private val VERTICES = floatArrayOf(
            -0.5f, 0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
            -0.5f, 0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f
        )

        private lateinit var CUBE: Mesh

        fun init() {
            CUBE = Mesh(INDICES, Mesh.VBO(VERTICES, 3))
            shader.create()
        }

        fun destroy() {
            shader.destroy()
            CUBE.destroy()
        }
    }
}