package io.posidon.potassium.world

import io.posidon.potassium.world.terrain.Terrain
import io.posidon.uraniumPotassium.content.Light
import io.posidon.uraniumPotassium.content.worldGen.Constants
import kotlin.system.measureTimeMillis

class LightManager(val terrain: Terrain) {

    fun updateGlobalLight(sx: Int, sz: Int, ex: Int, ez: Int) {

    }

    fun updateChunkSpotLights(chunkAccessor: Terrain.UnsafeChunkAccessor, x: Int, y: Int, z: Int) {
        measureTimeMillis {
            val chunk = chunkAccessor.getLoadedChunk(x, y, z) ?: return
            for ((i, light) in chunk.lightNodes) {
                val l = light.toInt()
                val absX = i / Constants.CHUNK_SIZE / Constants.CHUNK_SIZE + x * Constants.CHUNK_SIZE
                val absY = (i / Constants.CHUNK_SIZE) % Constants.CHUNK_SIZE + y * Constants.CHUNK_SIZE
                val absZ = i % Constants.CHUNK_SIZE + z * Constants.CHUNK_SIZE
                val s = terrain.sizeInVoxels
                propagateLight(chunkAccessor, run {
                    (absX + 1) % s
                }, absY, absZ, l)
                propagateLight(chunkAccessor, run {
                    val t = (absX - 1) % s
                    if (t < 0) s + t else t
                }, absY, absZ, l)
                propagateLight(chunkAccessor, absX, absY, run {
                    (absZ + 1) % s
                }, l)
                propagateLight(chunkAccessor, absX, absY, run {
                    val t = (absZ - 1) % s
                    if (t < 0) s + t else t
                }, l)
                propagateLight(chunkAccessor, absX, (absY + 1).coerceAtMost(terrain.heightInVoxels - 1), absZ, l)
                propagateLight(chunkAccessor, absX, (absY - 1).coerceAtLeast(0), absZ, l)
            }
        }.also { if (it > 10L) println("updateChunkSpotLights: $it ms") }
    }

    private tailrec fun propagateLight(chunkAccessor: Terrain.UnsafeChunkAccessor, wx: Int, wy: Int, wz: Int, light: Int) {
        val chunk = chunkAccessor.getLoadedChunk(wx / Constants.CHUNK_SIZE, wy / Constants.CHUNK_SIZE, wz / Constants.CHUNK_SIZE) ?: return
        val li = (wx % Constants.CHUNK_SIZE) * Constants.CHUNK_SIZE * Constants.CHUNK_SIZE + (wy % Constants.CHUNK_SIZE) * Constants.CHUNK_SIZE + wz % Constants.CHUNK_SIZE
        val block = chunk[li]
        if (block != null && block.isOpaque) {
            return
        }
        val blockLight = chunk.lightLevels[li].toInt()
        if (Light.lightI(light) <= Light.lightI(blockLight)
                && light and Light.R_MASK <= blockLight and Light.R_MASK
                && light and Light.G_MASK <= blockLight and Light.G_MASK
                && light and Light.B_MASK <= blockLight and Light.B_MASK) {
            return
        }
        val a = Light.blendLights(light, blockLight)
        //println("intensity: " + (Light.lightI(a) shr 12) + ", color: " + Light.lightColor(a))
        chunk.lightLevels[li] = a.toShort()
        //println(Light.lightI(a))
        val l = Light.dec(light)
        if (l == 0) {
            return
        }
        val s = terrain.sizeInVoxels
        propagateLight(chunkAccessor, run {
            (wx + 1) % s
        }, wy, wz, l)
        propagateLight(chunkAccessor, run {
            val t = (wx - 1) % s
            if (t < 0) s + t else t
        }, wy, wz, l)
        propagateLight(chunkAccessor, wx, wy, run {
            (wz + 1) % s
        }, l)
        propagateLight(chunkAccessor, wx, wy, run {
            val t = (wz - 1) % s
            if (t < 0) s + t else t
        }, l)
        propagateLight(chunkAccessor, wx, (wy + 1).coerceAtMost(terrain.heightInVoxels - 1), wz, l)
        propagateLight(chunkAccessor, wx, (wy - 1).coerceAtLeast(0), wz, l)
    }
}