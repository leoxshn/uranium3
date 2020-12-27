package posidon.uranium.voxel

import posidon.library.types.Vec2f
import posidon.library.types.Vec3i
import posidon.uranium.graphics.Mesh
import posidon.uranium.graphics.Renderer
import kotlin.math.sqrt

abstract class VoxelChunk<V : Voxel>(
    val position: Vec3i,
    val chunkMap: VoxelChunkMap<V, *>
) {

    inline val size get() = chunkMap.chunkSize

    private var willBeRendered = false
    var useCounter = 0

    var mesh: Mesh? = null
        private set

    val isVisible get() = willBeRendered

    val blocks = arrayOfNulls<Voxel>(size * size * size)

    operator fun get(pos: Vec3i): V? = get(pos.z, pos.y, pos.z)
    operator fun get(x: Int, y: Int, z: Int): V? = blocks[x * size * size + y * size + z] as V?
    operator fun set(pos: Vec3i, voxel: V?) = set(pos.x, pos.y, pos.z, voxel)
    operator fun set(x: Int, y: Int, z: Int, voxel: V?) { blocks[x * size * size + y * size + z] = voxel }

    inline fun withNeighborsLoaded(fn: () -> Unit) {
        val isTopChunk = position.y == chunkMap.sizeInChunks - 1
        val isBottomChunk = position.y == 0
        val bx = chunkMap[position.copy(x = chunkMap.clipChunkHorizontal(position.x + 1))]
        val sx = chunkMap[position.copy(x = chunkMap.clipChunkHorizontal(position.x - 1))]
        val bz = chunkMap[position.copy(z = chunkMap.clipChunkHorizontal(position.z + 1))]
        val sz = chunkMap[position.copy(z = chunkMap.clipChunkHorizontal(position.z - 1))]
        if (bx != null && sx != null && bz != null && sz != null) {
            bx.useCounter++
            sx.useCounter++
            bz.useCounter++
            sz.useCounter++
            if ((isTopChunk || chunkMap[position.copy(y = position.y + 1)] != null)) {
                val by = if (isTopChunk) null else chunkMap[position.copy(y = position.y + 1)].apply { useCounter++ }
                if ((isBottomChunk || chunkMap[position.copy(y = position.y - 1)] != null)) {
                    val sy = if (isBottomChunk) null else chunkMap[position.copy(y = position.y - 1)].apply { useCounter++ }
                    fn()
                    sy?.useCounter?.dec()
                }
                by?.useCounter?.dec()
            }
            bx.useCounter--
            sx.useCounter--
            bz.useCounter--
            sz.useCounter--
        }
    }

    fun generateMesh() {

        data class VoxelFace(
            val uv: Vec2f,
            var transparent: Boolean,
            var side: Int
        ) {
            fun isSame(face: VoxelFace) = face.uv == uv
        }

        fun getVoxelFace(x: Int, y: Int, z: Int, side: Int): VoxelFace? {
            val chunk = when {
                y >= size ->
                    if (position.y == chunkMap.heightInChunks - 1) return null
                    else chunkMap[position.copy(y = position.y + 1)]!!
                y < 0 ->
                    if (position.y == 0) return null
                    else chunkMap[position.copy(y = position.y - 1)]!!
                x >= size -> chunkMap[position.copy(x = chunkMap.clipChunkHorizontal(position.x + 1))]!!
                z >= size -> chunkMap[position.copy(z = chunkMap.clipChunkHorizontal(position.z + 1))]!!
                x < 0 -> chunkMap[position.copy(x = chunkMap.clipChunkHorizontal(position.x - 1))]!!
                z < 0 -> chunkMap[position.copy(z = chunkMap.clipChunkHorizontal(position.z - 1))]!!
                else -> return this[x, y, z]?.let {
                    VoxelFace(it.getUV(), false, side)
                }
            }

            val rx = x % size
            val ry = y % size
            val rz = z % size

            val smallX = if (rx < 0) size + rx else rx
            val smallY = if (ry < 0) size + ry else ry
            val smallZ = if (rz < 0) size + rz else rz

            return chunk[smallX, smallY, smallZ]?.let {
                VoxelFace(it.getUV(), false, side)
            }
        }

        val vertices = ArrayList<Float>()
        val indices = ArrayList<Int>()
        val uv = ArrayList<Float>()
        val normals = ArrayList<Float>()

        var side = 0
        val x = intArrayOf(0, 0, 0)
        val q = intArrayOf(0, 0, 0)
        val du = intArrayOf(0, 0, 0)
        val dv = intArrayOf(0, 0, 0)

        val mask = arrayOfNulls<VoxelFace>(size * size)

        var voxelFace: VoxelFace?
        var voxelFace1: VoxelFace?

        /**
         * We start with the lesser-spotted boolean for-loop (also known as the old flippy floppy).
         * The variable backFace will be TRUE on the first iteration and FALSE on the second - this allows
         * us to track which direction the indices should run during creation of the quad.
         * This loop runs twice, and the inner loop 3 times - totally 6 iterations - one for each
         * voxel face.
         */
        var backFace = true
        var b = false
        while (b != backFace) {
            for (d in 0..2) {
                val u = (d + 1) % 3
                val v = (d + 2) % 3
                x[0] = 0
                x[1] = 0
                x[2] = 0
                q[0] = 0
                q[1] = 0
                q[2] = 0
                q[d] = 1
                when (d) {
                    0 -> side = if (backFace) WEST else EAST
                    1 -> side = if (backFace) BOTTOM else TOP
                    2 -> side = if (backFace) SOUTH else NORTH
                }
                x[d] = -1
                while (x[d] < size) {
                    var n = 0
                    x[v] = 0
                    while (x[v] < size) {
                        x[u] = 0
                        while (x[u] < size) {
                            voxelFace = getVoxelFace(x[0], x[1], x[2], side)
                            voxelFace1 = getVoxelFace(x[0] + q[0], x[1] + q[1], x[2] + q[2], side)
                            mask[n++] = when {
                                voxelFace != null && voxelFace1 != null && voxelFace.isSame(voxelFace1) -> null
                                backFace -> voxelFace1
                                else -> voxelFace
                            }
                            x[u]++
                        }
                        x[v]++
                    }
                    x[d]++

                    n = 0
                    var j = 0
                    while (j < size) {
                        var i = 0
                        while (i < size) {
                            if (mask[n] != null) {
                                var w = 1
                                while (i + w < size && mask[n + w] != null && mask[n + w]!!.isSame(mask[n]!!)) {
                                    w++
                                }

                                var done = false
                                var h = 1
                                var k: Int
                                while (j + h < size) {
                                    k = 0
                                    while (k < w) {
                                        if (mask[n + k + h * size] == null || !mask[n + k + h * size]!!.isSame(mask[n]!!)) {
                                            done = true
                                            break
                                        }
                                        k++
                                    }
                                    if (done) {
                                        break
                                    }
                                    h++
                                }

                                /*
                                 * Here we check the "transparent" attribute in the VoxelFace class to ensure that we don't mesh
                                 * any culled faces.
                                 */
                                if (!mask[n]!!.transparent) {
                                    x[u] = i
                                    x[v] = j
                                    du[0] = 0
                                    du[1] = 0
                                    du[2] = 0
                                    du[u] = w
                                    dv[0] = 0
                                    dv[1] = 0
                                    dv[2] = 0
                                    dv[v] = h


                                    /// ADD QUAD TO LIST

                                    val minIndex = uv.size / 2

                                    fun addPoint(x: Int, y: Int, z: Int, nx: Int, ny: Int, nz: Int, voxelUv: Vec2f) {
                                        vertices.add(x.toFloat())
                                        vertices.add(y.toFloat())
                                        vertices.add(z.toFloat())

                                        val (uvX, uvY) = voxelUv
                                        uv.add(uvX)
                                        uv.add(uvY)

                                        normals.add(nx.toFloat())
                                        normals.add(ny.toFloat())
                                        normals.add(nz.toFloat())
                                    }

                                    var nx = du[1] * dv[2] - du[2] * dv[1]
                                    var ny = du[2] * dv[0] - du[0] * dv[2]
                                    var nz = du[0] * dv[1] - du[1] * dv[0]
                                    val length = sqrt(nx * nx + ny * ny + nz * nz.toDouble()).toFloat()

                                    if (backFace) {
                                        indices.add(2 + minIndex)
                                        indices.add(0 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(3 + minIndex)
                                        indices.add(2 + minIndex)

                                        nx = -(nx / length).toInt()
                                        ny = -(ny / length).toInt()
                                        nz = -(nz / length).toInt()
                                    } else {
                                        indices.add(2 + minIndex)
                                        indices.add(3 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(0 + minIndex)
                                        indices.add(2 + minIndex)

                                        nx = (nx / length).toInt()
                                        ny = (ny / length).toInt()
                                        nz = (nz / length).toInt()
                                    }

                                    val blockUv = mask[n]!!.uv

                                    addPoint(x[0], x[1], x[2], nx, ny, nz, blockUv)
                                    addPoint(x[0] + dv[0], x[1] + dv[1], x[2] + dv[2], nx, ny, nz, blockUv)
                                    addPoint(x[0] + du[0], x[1] + du[1], x[2] + du[2], nx, ny, nz, blockUv)
                                    addPoint(x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2], nx, ny, nz, blockUv)
                                }

                                /*
                                 * We zero out the mask
                                 */
                                var l = 0
                                while (l < h) {
                                    k = 0
                                    while (k < w) {
                                        mask[n + k + l * size] = null
                                        ++k
                                    }
                                    ++l
                                }

                                /*
                                 * And then finally increment the counters and continue
                                 */
                                i += w
                                n += w
                            } else {
                                i++
                                n++
                            }
                        }
                        j++
                    }
                }
            }
            backFace = backFace && b
            b = !b
        }

        val tmpVertices = vertices.toFloatArray()
        val tmpIndices = indices.toIntArray()
        val tmpUv = uv.toFloatArray()
        val tmpNormals = normals.toFloatArray()

        Renderer.runOnThread {
            val oldMesh = mesh
            if (tmpVertices.isEmpty()) {
                mesh = null
                willBeRendered = false
            } else {
                mesh = Mesh(tmpIndices, Mesh.VBO(3, *tmpVertices), Mesh.VBO(2, *tmpUv), Mesh.VBO(3, *tmpNormals))
                willBeRendered = true
            }
            oldMesh?.destroy()
        }
    }

    fun destroy() {
        willBeRendered = false
        Renderer.runOnThread {
            mesh?.destroy()
        }
    }

    companion object {
        private const val SOUTH = 0
        private const val NORTH = 1
        private const val EAST = 2
        private const val WEST = 3
        private const val TOP = 4
        private const val BOTTOM = 5
    }
}