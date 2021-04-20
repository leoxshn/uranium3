package io.posidon.uranium.util

import org.lwjgl.system.MemoryUtil
import java.nio.Buffer

object Heap {
    inline fun callocInt(size: Int) = MemoryUtil.memCallocInt(size)
    inline fun callocShort(size: Int) = MemoryUtil.memCallocShort(size)
    inline fun callocLong(size: Int) = MemoryUtil.memCallocLong(size)
    inline fun callocFloat(size: Int) = MemoryUtil.memCallocFloat(size)
    inline fun callocPointer(size: Int) = MemoryUtil.memCallocPointer(size)
    inline fun calloc(size: Int) = MemoryUtil.memCalloc(size)

    inline fun mallocInt(size: Int) = MemoryUtil.memAllocInt(size)
    inline fun mallocLong(size: Int) = MemoryUtil.memAllocLong(size)
    inline fun mallocFloat(size: Int) = MemoryUtil.memAllocFloat(size)
    inline fun mallocPointer(size: Int) = MemoryUtil.memAllocPointer(size)
    inline fun malloc(size: Int) = MemoryUtil.memAlloc(size)

    inline fun int(vararg v: Int) = MemoryUtil.memAllocInt(v.size).put(v).flip()
    inline fun long(vararg v: Long) = MemoryUtil.memAllocLong(v.size).put(v).flip()
    inline fun float(vararg v: Float) = MemoryUtil.memAllocFloat(v.size).put(v).flip()

    inline fun free(buff: Buffer) = MemoryUtil.memFree(buff)

    inline operator fun get(address: Long) = MemoryUtil.memGetByte(address)
    inline fun getIntBuffer(address: Long, capacity: Int) = MemoryUtil.memIntBuffer(address, capacity)
}

val Buffer.address: Long get() = MemoryUtil.memAddress(this)