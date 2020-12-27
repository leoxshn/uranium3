package posidon.uranium.voxel

import org.lwjgl.opengl.GL11C
import posidon.library.types.Vec3f
import posidon.library.types.Vec3i
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Shader
import posidon.uranium.nodes.Node
import posidon.uranium.nodes.spatial.BoundingBox
import posidon.uranium.nodes.spatial.Collider
import posidon.uranium.nodes.spatial.Eye

abstract class VoxelChunkMap<V : Voxel, C : VoxelChunk<V>> : Node(), Collider {

    val sizeInChunks = 16
    val heightInChunks = 8

    inline val sizeInVoxels get() = sizeInChunks * chunkSize

    abstract val chunkSize: Int

    companion object {
        private val blockShader = Shader("/shaders/blockVertex.glsl", "/shaders/blockFragment.glsl").also { Renderer.runOnThread { it.create() } }

        internal fun destroy() {
            blockShader.destroy()
        }
    }

    private val array = arrayOfNulls<VoxelChunk<V>>(sizeInChunks * sizeInChunks * heightInChunks)

    fun clipChunkHorizontal(x: Int): Int {
        val r = x % sizeInChunks
        return if (r < 0) sizeInChunks + r else r
    }
    fun clipChunkHorizontal(x: Float): Float {
        val r = x % sizeInChunks
        return if (r < 0) sizeInChunks + r else r
    }
    fun clipVoxelHorizontal(x: Int): Int {
        val v = sizeInVoxels
        val r = x % v
        return if (r < 0) v + r else r
    }
    fun clipVoxelHorizontal(x: Float): Float {
        val v = sizeInVoxels
        val r = x % v
        return if (r < 0) v + r else r
    }

    operator fun get(x: Int, y: Int, z: Int): C? {
        when {
            x < 0 || x >= sizeInChunks -> throw IllegalArgumentException("x = $x")
            z < 0 || z >= sizeInChunks -> throw IllegalArgumentException("z = $z")
            y < 0 || y >= heightInChunks -> throw IllegalArgumentException("y = $y")
        }
        return array[x * sizeInChunks * heightInChunks + y * sizeInChunks + z] as C?
    }
    operator fun set(x: Int, y: Int, z: Int, chunk: C?) { array[x * sizeInChunks * heightInChunks + y * sizeInChunks + z] = chunk }
    operator fun get(v: Vec3i): C? = get(v.x, v.y, v.z)
    operator fun set(v: Vec3i, chunk: C) = set(v.x, v.y, v.z, chunk)

    override fun render(renderer: Renderer, eye: Eye) {
        GL11C.glEnable(GL11C.GL_DEPTH_TEST)

        blockShader.bind()
        blockShader["view"] = eye.viewMatrix
        blockShader["projection"] = Renderer.projectionMatrix

        preRender(blockShader)

        for (_x in 0 until sizeInChunks) for (_z in 0 until sizeInChunks) for (_y in 0 until heightInChunks) {
            val chunk = get(_x, _y, _z)
            if (chunk != null && chunk.isVisible /*&& chunk.isInFov(eye)*/) {
                blockShader["position"] = run {
                    val absolutePosition = Vec3f(_x * chunkSize.toFloat(), _y * chunkSize.toFloat(), _z * chunkSize.toFloat())
                    val eyePos = eye.globalTransform.position
                    var position = absolutePosition
                    for (x in -1..1) for (z in -1..1) {
                        val newPosition = Vec3f(x * sizeInVoxels.toFloat(), 0f, z * sizeInVoxels.toFloat()).apply {
                            selfAdd(absolutePosition)
                        }
                        if ((newPosition - eyePos).length < (position - eyePos).length) position = newPosition
                    }
                    position
                }
                Renderer.render(chunk.mesh!!)
            }
        }
    }

    open fun preRender(shader: Shader) {}

    override fun destroy() {
        for (chunk in array) chunk?.destroy()
    }

    fun getBlock(position: Vec3i) = getBlock(position.x, position.y, position.z)
    fun getBlock(x: Int, y: Int, z: Int): V? =
        this[x / chunkSize, y / chunkSize, z / chunkSize]
            ?.get(x % chunkSize, y % chunkSize, z % chunkSize)
    fun setBlock(position: Vec3i, voxel: V?) = setBlock(position.x, position.y, position.z, voxel)
    fun setBlock(x: Int, y: Int, z: Int, voxel: V?): C? =
        this[x / chunkSize, y / chunkSize, z / chunkSize]?.apply {
            set(x % chunkSize, y % chunkSize, z % chunkSize, voxel)
        }

    override fun collide(point: Vec3f): Boolean {
        return point.y != 0f && point.y != heightInChunks - 1f && getBlock(point.x.toInt(), point.y.toInt(), point.z.toInt()) != null
    }

    override fun collide(boundingBox: BoundingBox): Boolean {
        val origin = boundingBox.getRealOrigin()
        val o = origin.floorToVec3i()
        val e = (origin + boundingBox.size).floorToVec3i()

        for (x in o.x..e.x) for (y in o.y..e.y) for (z in o.z..e.z) {
            if (y >= 0 && y < heightInChunks * chunkSize && getBlock(clipVoxelHorizontal(x), y, clipVoxelHorizontal(z)) != null) {
                return true
            }
        }

        return false
    }
}