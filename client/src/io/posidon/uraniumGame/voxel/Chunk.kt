package io.posidon.uraniumGame.voxel

import io.posidon.library.util.set
import io.posidon.uranium.graphics.Mesh
import io.posidon.uranium.util.*
import io.posidon.uraniumPotassium.content.Light
import io.posidon.uraniumPotassium.content.worldGen.Constants
import io.posidon.uraniumPotassium.content.worldGen.Constants.CHUNK_SIZE_CUBE
import java.nio.ByteBuffer
import kotlin.math.sqrt

class Chunk (
    val chunkX: Int,
    val chunkY: Int,
    val chunkZ: Int
) {
    companion object {

        private const val SOUTH = 0
        private const val NORTH = 1
        private const val EAST = 2
        private const val WEST = 3
        private const val TOP = 4
        private const val BOTTOM = 5

        inline val size get() = Constants.CHUNK_SIZE

        const val _light_I = 0
        const val _voxelData_I = _light_I + CHUNK_SIZE_CUBE * Short.SIZE_BYTES // light
        const val _FULL_SIZE = _voxelData_I + CHUNK_SIZE_CUBE * Long.SIZE_BYTES // voxelData
    }

    var useCounter = 0
    var willBeRendered = false
    var mesh: Mesh? = null

    val memory = Heap.calloc(_FULL_SIZE)

    inline fun getLight(i: Int): Short = memory.getShort(_light_I + checkRange(i) * Short.SIZE_BYTES)
    inline fun setLight(i: Int, v: Short) { memory.putShort(_light_I + checkRange(i) * Short.SIZE_BYTES, v) }

    inline fun setVoxel(i: Int, v: Voxel?) {
        memory.putLong(_voxelData_I + checkRange(i) * Long.SIZE_BYTES, v?.uv?.address ?: 0)
    }
    inline fun getVoxel(i: Int): Voxel? = memory.getLong(_voxelData_I + checkRange(i) * Long.SIZE_BYTES).let {
        if (it == 0L) null else Voxel(Heap.getIntBuffer(it, Voxel.SIZE_BYTES))
    }

    inline fun checkRange(i: Int) = if (i >= CHUNK_SIZE_CUBE)
        throw IndexOutOfBoundsException(i)
    else i

    inline operator fun get(x: Int, y: Int, z: Int): Voxel? = getVoxel(x * size * size + y * size + z)
    inline operator fun set(x: Int, y: Int, z: Int, voxel: Voxel?) {
        setVoxel(x * size * size + y * size + z, voxel)
    }

    inline fun withNeighborsLoaded(chunkMap: ChunkMap, fn: () -> Unit) {
        chunkMap.checkLock.lock()
        val isTopChunk = chunkY == ChunkMap.heightInChunks - 1
        val isBottomChunk = chunkY == 0
        val bx = chunkMap[ChunkMap.clipChunkHorizontal(chunkX + 1), chunkY, chunkZ]
        val sx = chunkMap[ChunkMap.clipChunkHorizontal(chunkX - 1), chunkY, chunkZ]
        val bz = chunkMap[chunkX, chunkY, ChunkMap.clipChunkHorizontal(chunkZ + 1)]
        val sz = chunkMap[chunkX, chunkY, ChunkMap.clipChunkHorizontal(chunkZ - 1)]
        if (bx != null && sx != null && bz != null && sz != null) {
            bx.useCounter++
            sx.useCounter++
            bz.useCounter++
            sz.useCounter++
            if ((isTopChunk || chunkMap[chunkX, chunkY + 1, chunkZ] != null)) {
                val by = if (isTopChunk) null else chunkMap[chunkX, chunkY + 1, chunkZ].apply { useCounter++ }
                if ((isBottomChunk || chunkMap[chunkX, chunkY - 1, chunkZ] != null)) {
                    val sy = if (isBottomChunk) null else chunkMap[chunkX, chunkY - 1, chunkZ].apply { useCounter++ }
                    fn()
                    sy?.useCounter?.dec()
                    chunkMap.checkLock.unlock()
                } else chunkMap.checkLock.unlock()
                by?.useCounter?.dec()
            } else chunkMap.checkLock.unlock()
            bx.useCounter--
            sx.useCounter--
            bz.useCounter--
            sz.useCounter--
        } else chunkMap.checkLock.unlock()
    }
/*
    private class VoxelFace(
        val uvX: Int,
        val uvY: Int,
        var transparent: Boolean,
        var side: Int,
        var light: Short,
    ) {
        val memory = Heap.malloc(1)
        inline fun isSame(face: VoxelFace) =
            face.uvX == uvX &&
            face.uvY == uvY &&
            face.light == light
    }*/

    private inline class VoxelFace(
        val memory: ByteBuffer
    ) {
        companion object {
            const val SIZE_BYTES = Int.SIZE_BYTES * 2 + 2 + Float.SIZE_BYTES * 3

            inline operator fun invoke(
                uvX: Int,
                uvY: Int,
                transparent: Boolean,
                side: Int,
                light: Short
            ) = VoxelFace(Heap.malloc(SIZE_BYTES).apply {
                putInt(0, uvX)
                putInt(Int.SIZE_BYTES, uvY)
                put(Int.SIZE_BYTES * 2, if (transparent) 1 else 0)
                put(Int.SIZE_BYTES * 2 + 1, side.toByte())

                val light = light.toInt()
                val lightMultiplier = (Light.lightI(light) shr 12) / 15f / 15f
                val R = ((light and Light.R_MASK)) * lightMultiplier
                val G = ((light and Light.G_MASK) shr 4) * lightMultiplier
                val B = ((light and Light.B_MASK) shr 8) * lightMultiplier

                putFloat(Int.SIZE_BYTES * 2 + 2, R)
                putFloat(Int.SIZE_BYTES * 2 + 2 + Float.SIZE_BYTES, G)
                putFloat(Int.SIZE_BYTES * 2 + 2 + Float.SIZE_BYTES * 2, B)
            })
        }

        inline val uvX: Int get() = memory.getInt(0)
        inline val uvY: Int get() = memory.getInt(Int.SIZE_BYTES)
        inline val transparent: Boolean get() = memory.get(Int.SIZE_BYTES * 2) != 0.toByte()
        inline val side: Int get() = memory.get(Int.SIZE_BYTES * 2 + 1).toInt()
        inline val light: Short get() = memory.getShort(Int.SIZE_BYTES * 2 + 2)
        inline val R: Float get() = memory.getFloat(Int.SIZE_BYTES * 2 + 2)
        inline val G: Float get() = memory.getFloat(Int.SIZE_BYTES * 2 + 2 + Float.SIZE_BYTES)
        inline val B: Float get() = memory.getFloat(Int.SIZE_BYTES * 2 + 2 + Float.SIZE_BYTES * 2)

        inline fun isSame(face: VoxelFace) =
            face.uvX == uvX &&
            face.uvY == uvY &&
            face.R == R &&
            face.G == G &&
            face.B == B
    }

    fun generateMesh(chunkMap: ChunkMap) {

        fun getVoxelFace(x: Int, y: Int, z: Int, side: Int): VoxelFace? {

            fun getAdjacentLight(_x: Int, _y: Int, _z: Int, side: Int, chunk: Chunk): Short {
                var x = _x
                var y = _y
                var z = _z
                when (side) {
                    SOUTH -> z--
                    NORTH -> z++
                    EAST -> x++
                    WEST -> x--
                    TOP -> y++
                    BOTTOM -> y--
                    else -> return 0
                }
                val realChunk = when {
                    y >= size ->
                        if (chunkY == ChunkMap.heightInChunks - 1) return 0
                        else chunkMap[chunkX, chunkY + 1, chunkZ]!!
                    y < 0 ->
                        if (chunkY == 0) return 0
                        else chunkMap[chunkX, chunkY - 1, chunkZ]!!
                    x >= size -> chunkMap[ChunkMap.clipChunkHorizontal(chunkX + 1), chunkY, chunkZ]!!
                    z >= size -> chunkMap[chunkX, chunkY, ChunkMap.clipChunkHorizontal(chunkZ + 1)]!!
                    x < 0 -> chunkMap[ChunkMap.clipChunkHorizontal(chunkX - 1), chunkY, chunkZ]!!
                    z < 0 -> chunkMap[chunkX, chunkY, ChunkMap.clipChunkHorizontal(chunkZ - 1)]!!
                    else -> return chunk.getLight(x * size * size + y * size + z)
                }
                val rx = x % size
                val ry = y % size
                val rz = z % size
                val smallX = if (rx < 0) size + rx else rx
                val smallY = if (ry < 0) size + ry else ry
                val smallZ = if (rz < 0) size + rz else rz
                return realChunk.getLight(smallX * size * size + smallY * size + smallZ)
            }

            val chunk = when {
                y >= size ->
                    if (chunkY == ChunkMap.heightInChunks - 1) return null
                    else chunkMap[chunkX, chunkY + 1, chunkZ]!!
                y < 0 ->
                    if (chunkY == 0) return null
                    else chunkMap[chunkX, chunkY - 1, chunkZ]!!
                x >= size -> chunkMap[ChunkMap.clipChunkHorizontal(chunkX + 1), chunkY, chunkZ]!!
                z >= size -> chunkMap[chunkX, chunkY, ChunkMap.clipChunkHorizontal(chunkZ + 1)]!!
                x < 0 -> chunkMap[ChunkMap.clipChunkHorizontal(chunkX - 1), chunkY, chunkZ]!!
                z < 0 -> chunkMap[chunkX, chunkY, ChunkMap.clipChunkHorizontal(chunkZ - 1)]!!
                else -> return this[x, y, z]?.let {
                    VoxelFace(it.uvX, it.uvY, false, side, getAdjacentLight(x, y, z, side, this))
                }
            }

            val rx = x % size
            val ry = y % size
            val rz = z % size

            val smallX = if (rx < 0) size + rx else rx
            val smallY = if (ry < 0) size + ry else ry
            val smallZ = if (rz < 0) size + rz else rz

            return chunk[smallX, smallY, smallZ]?.let {
                VoxelFace(it.uvX, it.uvY, false, side, getAdjacentLight(smallX, smallY, smallZ, side, chunk))
            }
        }

        Stack.push { stack ->

            val vertices = NativeFloatList.allocHeap(256)
            val indices = NativeIntList.allocHeap(256)
            val uv = NativeFloatList.allocHeap(256)
            val normals = NativeFloatList.allocHeap(256)
            val pointLight = NativeFloatList.allocHeap(256)

            var side = 0
            val x = stack.callocInt(3)
            val q = stack.callocInt(3)
            val du = stack.callocInt(3)
            val dv = stack.callocInt(3)

            val mask = arrayOfNulls<VoxelFace>(size * size)

            /**
             * We start with the lesser-spotted boolean for-loop (also known as the old flippy floppy).
             * The variable backFace will be TRUE on the first iteration and FALSE on the second - this allows
             * us to track which direction the indices should run during creation of the quad.
             * This loop runs twice, and the inner loop 3 times - totally 6 iterations - one for each
             * voxel face.
             */
            var a = 0

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
                                val voxelFace = getVoxelFace(x[0], x[1], x[2], side)
                                val voxelFace1 = getVoxelFace(x[0] + q[0], x[1] + q[1], x[2] + q[2], side)
                                a++
                                a++
                                mask[n]?.let { Heap.free(it.memory); a-- }
                                mask[n++] = when {
                                    voxelFace != null && voxelFace1 != null && voxelFace.isSame(voxelFace1) -> {
                                        Heap.free(voxelFace.memory)
                                        Heap.free(voxelFace1.memory)
                                        a--
                                        a--
                                        null
                                    }
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
                                            val m = mask[n + k + h * size]
                                            if (m == null || !m.isSame(mask[n]!!)) {
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
                                    val face = mask[n]!!
                                    if (!face.transparent) {
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

                                        fun addPoint(x: Int, y: Int, z: Int, nx: Int, ny: Int, nz: Int, uvX: Int, uvY: Int, R: Float, G: Float, B: Float) {
                                            vertices.add(x.toFloat())
                                            vertices.add(y.toFloat())
                                            vertices.add(z.toFloat())

                                            uv.add(uvX.toFloat())
                                            uv.add(uvY.toFloat())

                                            normals.add(nx.toFloat())
                                            normals.add(ny.toFloat())
                                            normals.add(nz.toFloat())

                                            pointLight.add(R)
                                            pointLight.add(G)
                                            pointLight.add(B)
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

                                        val uvX = face.uvX
                                        val uvY = face.uvY
                                        val R = face.R
                                        val G = face.G
                                        val B = face.B
                                        //if (R != 0f || G != 0f || B != 0f) println("light at ${x[0]}, ${x[1]}, ${x[2]} : $R, $G, $B")

                                        addPoint(x[0], x[1], x[2], nx, ny, nz, uvX, uvY, R, G, B)
                                        addPoint(x[0] + dv[0], x[1] + dv[1], x[2] + dv[2], nx, ny, nz, uvX, uvY, R, G, B)
                                        addPoint(x[0] + du[0], x[1] + du[1], x[2] + du[2], nx, ny, nz, uvX, uvY, R, G, B)
                                        addPoint(x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2], nx, ny, nz, uvX, uvY, R, G, B)
                                    }

                                    /*
                                     * We zero out the mask
                                     */
                                    var l = 0
                                    while (l < h) {
                                        k = 0
                                        while (k < w) {
                                            val ii = n + k + l * size
                                            mask[ii]?.let {
                                                mask[ii] = null
                                                Heap.free(it.memory)
                                                a--
                                            }
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

            //println(a)

            val oldMesh = mesh
            if (vertices.isEmpty()) {
                willBeRendered = false
                mesh = null
                vertices.free()
                indices.free()
                uv.free()
                normals.free()
                pointLight.free()
                chunkMap.renderer.runOnThread {
                    oldMesh?.destroy()
                }
            } else {
                val tmpVertices = vertices.toFloatArray()
                val tmpIndices = indices.toIntArray()
                val tmpUv = uv.toFloatArray()
                val tmpNormals = normals.toFloatArray()
                val tmpPointLight = pointLight.toFloatArray()
                chunkMap.renderer.runOnThread {
                    oldMesh?.destroy()
                    mesh = Mesh.make(tmpIndices,
                        Mesh.FloatVBO(3, *tmpVertices),
                        Mesh.FloatVBO(2, *tmpUv),
                        Mesh.FloatVBO(3, *tmpNormals),
                        Mesh.FloatVBO(3, *tmpPointLight))
                    willBeRendered = true
                    vertices.free()
                    indices.free()
                    uv.free()
                    normals.free()
                    pointLight.free()
                }
            }
        }
    }

    fun deleteMesh(chunkMap: ChunkMap) {
        willBeRendered = false
        chunkMap.renderer.runOnThread {
            mesh?.destroy()
            mesh = null
        }
    }

    fun destroy(chunkMap: ChunkMap) {
        deleteMesh(chunkMap)
        Heap.free(memory)
    }
}