package posidon.uranium.voxel

import org.lwjgl.opengl.GL11
import posidon.library.types.Vec3f
import posidon.library.types.Vec3i
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.BoundingBox
import posidon.uranium.nodes.spatial.Collider
import posidon.uranium.nodes.spatial.Eye
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

abstract class VoxelChunkMap<C : VoxelChunk<*>>(name: String) : Node(name), Collider {

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
        blockShader["sunNormal"] = Scene.environment.sun?.normal ?: Vec3f.ZERO
        blockShader["projection"] = Renderer.projectionMatrix

        preRender(blockShader)

        for (chunk in map.values) {
            if (chunk.willBeRendered /*&& chunk.isInFov(eye)*/) {
                blockShader["position"] = chunk.absolutePosition.toVec3f()
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

    fun getBlock(position: Vec3f) = getBlock(floor(position.x).toInt(), floor(position.y).toInt(), floor(position.z).toInt())
    fun getBlock(position: Vec3i) = getBlock(position.x, position.y, position.z)
    fun getBlock(x: Int, y: Int, z: Int): Voxel? {
        val smallX = if (x % chunkSize < 0) chunkSize + x % chunkSize else x % chunkSize
        val smallY = if (y % chunkSize < 0) chunkSize + y % chunkSize else y % chunkSize
        val smallZ = if (z % chunkSize < 0) chunkSize + z % chunkSize else z % chunkSize
        val chunkPos = Vec3i(floor(x.toFloat() / chunkSize).toInt(), floor(y.toFloat() / chunkSize).toInt(), floor(z.toFloat() / chunkSize).toInt())
        return this[chunkPos]?.get(smallX, smallY, smallZ)
    }

    override fun collide(point: Vec3f): Boolean {
        return getBlock(point.x.toInt(), point.y.toInt(), point.z.toInt()) != null
    }

    override fun collide(boundingBox: BoundingBox): Boolean {
        val o = boundingBox.getRealOrigin().roundToVec3i()
        val c = o + (boundingBox.size / 2f).roundToVec3i()
        val e = c + (boundingBox.size / 2f).roundToVec3i()

        //println("$o, $e")

        for (x in o.x..e.x) for (y in o.y..e.y) for (z in o.z..e.z) {
            if (getBlock(x, y, z) != null) {
                return true
            }
        }

        return false
    }
}