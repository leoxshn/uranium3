package posidon.uranium.engine.graphics

import org.lwjgl.opengl.*
import posidon.uranium.engine.nodes.spatial.voxel.Block
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.engine.nodes.spatial.Camera
import posidon.uranium.engine.nodes.spatial.voxel.ChunkMap
import posidon.uranium.engine.nodes.ui.View
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList

object Renderer {

    private var uiShader = Shader("/shaders/viewVertex.shade", "/shaders/viewFragment.shade")
    val ui = ArrayList<View>()
    val chunks = ChunkMap("chunks")
    val blocksToUpdate = ConcurrentLinkedQueue<Block>()
    val chunkLock = ReentrantLock()

    fun init() {

        ChunkMap.blockShader.create()

        uiShader.create()
        View.init()

        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
    }

    fun render(camera: Camera) {
        RootNode.render(this, camera)
        chunks.render(this, camera)
        renderUI()
    }

    fun renderUI() {
        uiShader.bind()
        GL30.glBindVertexArray(View.MESH.vaoId)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, View.MESH.getVbo(1))
        for (view in ui)
            if (view.visible)
                view.render(uiShader)
    }

    fun updateData() {
        runMainThreadEvents()
    }

    fun kill() {
        GL20.glUseProgram(0)
        ChunkMap.blockShader.destroy()
        uiShader.destroy()
        blocksToUpdate.clear()
        RootNode.destroy()
        View.destroyAll()
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