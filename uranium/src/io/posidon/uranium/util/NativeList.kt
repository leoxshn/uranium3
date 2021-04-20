package io.posidon.uranium.util

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*

class NativeFloatList (var buffer: FloatBuffer) {

    inline val size get() = buffer.position()

    inline fun add(v: Float) =
        if (buffer.hasRemaining()) buffer.put(v)
        else MemoryUtil.memRealloc(buffer, buffer.capacity() + 256).put(v).also { buffer = it }

    inline fun toFloatArray(): FloatArray {
        return if (buffer.hasArray()) Arrays.copyOf(buffer.array(), size)
        else FloatArray(buffer.position()) { buffer[it] }
    }

    inline fun free() = MemoryUtil.memFree(buffer)
    inline fun isEmpty(): Boolean = size == 0

    companion object {
        inline fun allocHeap(size: Int) = NativeFloatList(MemoryUtil.memAllocFloat(size))
    }
}

class NativeIntList (var buffer: IntBuffer) {

    inline val size get() = buffer.position()

    inline fun add(v: Int) =
        if (buffer.hasRemaining()) buffer.put(v)
        else MemoryUtil.memRealloc(buffer, buffer.capacity() + 256).put(v).also { buffer = it }

    inline fun toIntArray(): IntArray {
        return if (buffer.hasArray()) Arrays.copyOf(buffer.array(), size)
        else IntArray(buffer.position()) { buffer[it] }
    }

    inline fun free() = MemoryUtil.memFree(buffer)
    inline fun isEmpty(): Boolean = size == 0

    companion object {
        inline fun allocHeap(size: Int) = NativeIntList(MemoryUtil.memAllocInt(size))
    }
}

typealias PointerBuffer = PointerBuffer