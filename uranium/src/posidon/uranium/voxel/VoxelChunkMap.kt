package posidon.uranium.voxel

import org.lwjgl.opengl.GL11
import posidon.library.types.Vec3i
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.Eye
import java.util.concurrent.ConcurrentHashMap

abstract class VoxelChunkMap<C : VoxelChunk<*>>(name: String) : Node(name) {

    abstract val chunkSize: Int

    companion object {
        private val blockShader = Shader("/shaders/blockVertex.glsl", /*"/shaders/wireframeGeometry.glsl",*/ "/shaders/blockFragment.glsl")

        internal fun init() {
            blockShader.create()
        }

        internal fun destroy() {
            blockShader.destroy()
        }
    }

    protected val map = ConcurrentHashMap<Vec3i, C>()

    operator fun get(x: Int, y: Int, z: Int): C? = map[Vec3i(x, y, z)]
    operator fun set(x: Int, y: Int, z: Int, chunk: C) { map[Vec3i(x, y, z)] = chunk }
    operator fun get(v: Vec3i): C? = map[v]
    operator fun set(v: Vec3i, chunk: C) { map[v] = chunk }

    override fun render(renderer: Renderer, eye: Eye) {
        GL11.glEnable(GL11.GL_DEPTH_TEST)

        blockShader.bind()
        blockShader["ambientLight"] = Scene.environment.ambientLight
        blockShader["view"] = eye.viewMatrix
        blockShader["skyColor"] = Scene.environment.skyColor
        blockShader["skyLight"] = Scene.environment.skyLight
        blockShader["sunNormal"] = Scene.environment.sunNormal
        blockShader["projection"] = Renderer.projectionMatrix

        preRender(blockShader)

        for (chunk in map.values) {
            if (chunk.willBeRendered /*&& eye.isPositionInFov(chunk.position * Chunk.SIZE)*/) {
                blockShader["position"] = (chunk.position * chunkSize).toVec3f()
                Renderer.render(chunk.mesh!!)
            }
        }
    }

    open fun preRender(shader: Shader) {}

    override fun destroy() {
        for (key in map.keys) {
            map.remove(key)!!.destroy()
        }
    }
}