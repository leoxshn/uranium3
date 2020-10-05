package posidon.uranium.graphics

import org.lwjgl.opengl.*
import posidon.uranium.graphics.mesh.Mesh
import posidon.uranium.nodes.RootNode
import posidon.uranium.nodes.spatial.Camera
import posidon.uranium.nodes.ui.UIComponent
import java.util.concurrent.ConcurrentLinkedQueue

object Renderer {

    var camera: Camera? = null

    internal fun init() {
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
    }

    internal fun renderObjects() {
        camera?.let { RootNode.render(this, it) }
    }

    fun render(mesh: Mesh) {
        mesh.bind()
        GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.vertexCount, GL11.GL_UNSIGNED_INT, 0)
    }

    internal fun updateData() {
        runMainThreadEvents()
    }

    internal fun kill() {
        GL20.glUseProgram(0)
        UIComponent.shader.destroy()
    }

    private val mainThreadQueue = ConcurrentLinkedQueue<() -> Unit>()

    fun runOnMainThread(fn: () -> Unit) {
        mainThreadQueue.add(fn)
    }

    private fun runMainThreadEvents() {
        val it = mainThreadQueue.iterator()
        while (it.hasNext()) {
            it.next()()
            it.remove()
        }
    }
}