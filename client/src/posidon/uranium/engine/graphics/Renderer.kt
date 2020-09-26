package posidon.uranium.engine.graphics

import org.lwjgl.opengl.*
import posidon.uranium.engine.nodes.spatial.voxel.Block
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.engine.nodes.spatial.Camera
import posidon.uranium.engine.nodes.spatial.voxel.ChunkMap
import posidon.uranium.engine.nodes.ui.UIComponent
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock

object Renderer {

    var camera: Camera? = null
    val blocksToUpdate = ConcurrentLinkedQueue<Block>()
    val chunkLock = ReentrantLock()

    fun init() {
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
    }

    fun render() {
        camera?.let { RootNode.render(this, it) }
    }

    fun updateData() {
        runMainThreadEvents()
    }

    fun kill() {
        GL20.glUseProgram(0)
        ChunkMap.blockShader.destroy()
        UIComponent.shader.destroy()
        blocksToUpdate.clear()
        RootNode.destroy()
        BlockTextures.clear()
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