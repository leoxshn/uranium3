package posidon.uranium.graphics

import org.lwjgl.opengl.*
import posidon.uranium.gameLoop.GameLoop
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Camera
import posidon.uranium.nodes.ui.UIComponent
import java.util.concurrent.ConcurrentLinkedQueue

object Renderer {

    const val NEAR = 0.2f
    const val FAR = 600f

    /**
     * The camera from whose point of view the renderer is gonna render 3d objects.
     * [projectionMatrix]'s fov gets set to the camera's
     */
    var camera: Camera? = null
        set(value) {
            field = value
            camera?.fov?.let { projectionMatrix.setFovAndAspectRatio(it, Window.width.toFloat() / Window.height.toFloat()) }
        }

    /**
     * Projection matrix used for all 3d rendering.
     * It's fov depends on the [camera]
     */
    val projectionMatrix = ProjectionMatrix(0f, 0f, NEAR, FAR)

    /**
     * Mesh of a quad.
     * Used for [UIComponent]s
     */
    lateinit var QUAD_MESH: Mesh private set

    /**
     * Renders [mesh]
     */
    fun render(mesh: Mesh) {
        mesh.bind()
        GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.vertexCount, GL11.GL_UNSIGNED_INT, 0)
    }

    /**
     * Adds [fn] to the queue to run on the render thread
     */
    fun runOnThread(fn: () -> Unit) {
        eventQueue.add(fn)
    }

    internal fun init() {
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)

        QUAD_MESH = Mesh(intArrayOf(0, 1, 3, 3, 1, 2), listOf(Mesh.VBO(floatArrayOf(
            -1f, 1f,
            -1f, -1f,
            1f, -1f,
            1f, 1f
        ), 2)))
    }

    internal fun kill() {
        GL20.glUseProgram(0)
        UIComponent.shader.destroy()
    }

    private val eventQueue = ConcurrentLinkedQueue<() -> Unit>()

    internal fun loop() {
        while (Window.isOpen && GameLoop.running) {
            Window.update()
            camera?.let { Scene.render(this, it) }
            Window.swapBuffers()

            val it = eventQueue.iterator()
            while (it.hasNext()) {
                it.next()()
                it.remove()
            }
        }
    }
}