package io.posidon.uranium.graphics

import io.posidon.library.util.set
import io.posidon.uranium.nodes.ui.View
import io.posidon.uranium.util.Heap
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.FloatBuffer
import java.nio.IntBuffer

inline class Mesh(
    val memory: IntBuffer
) {

    inline val vertexCount get() = memory[1]
    inline val vboCount get() = memory[2]

    internal inline fun bind() {
        GL30.glBindVertexArray(memory[0])
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, memory[HEADER_SIZE_IN_BYTES])
    }

    fun destroy() {
        bind()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        for (i in 0 until vboCount) {
            val vboId = memory[i + HEADER_SIZE_IN_BYTES + 1]
            GL20.glDisableVertexAttribArray(i)
            GL15.glDeleteBuffers(vboId)
        }
        GL15.glDeleteBuffers(memory[HEADER_SIZE_IN_BYTES])
        GL30.glBindVertexArray(0)
        GL30.glDeleteVertexArrays(memory[0])
        Heap.free(memory)
    }

    companion object {

        /**
         * Mesh of a quad.
         * Used for [View]s
         */
        lateinit var QUAD_MEMORY: IntBuffer private set
        inline val QUAD get() = Mesh(QUAD_MEMORY)
        const val HEADER_SIZE_IN_BYTES = 3

        fun init() {
            QUAD_MEMORY = makeMesh(intArrayOf(0, 1, 3, 3, 1, 2), FloatVBO(size = 2,
                -1f, 1f,
                -1f, -1f,
                1f, -1f,
                1f, 1f
            ))
        }

        fun destroy() {
            QUAD.destroy()
        }

        fun make(indices: IntArray, vararg vbos: VBO): Mesh = Mesh(makeMesh(indices, *vbos))
        inline fun fromAddress(address: Long): Mesh {
            val header = Heap.getIntBuffer(address, HEADER_SIZE_IN_BYTES)
            return Mesh(Heap.getIntBuffer(address, HEADER_SIZE_IN_BYTES + 1 + header[2]))
        }

        private fun makeMesh(indices: IntArray, vararg vbos: VBO): IntBuffer {
            val memory = Heap.mallocInt(HEADER_SIZE_IN_BYTES + 1 + vbos.size)
            var indicesBuffer: IntBuffer? = null
            try {
                memory[0] = GL30.glGenVertexArrays()
                memory[1] = indices.size
                memory[2] = vbos.size
                GL30.glBindVertexArray(memory[0])

                // Index VBO
                val vboId = GL15.glGenBuffers()
                memory[HEADER_SIZE_IN_BYTES] = vboId
                indicesBuffer = Heap.mallocInt(indices.size)
                indicesBuffer.put(indices).flip()
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId)
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)

                for (i in vbos.indices) {
                    memory[HEADER_SIZE_IN_BYTES + 1 + i] = vbos[i].bind(i)
                }
            } finally {
                if (indicesBuffer != null) Heap.free(indicesBuffer)
            }
            return memory
        }
    }

    abstract class VBO {
        abstract val size: Int
        abstract fun bind(i: Int): Int
    }

    class FloatVBO (
        override val size: Int,
        vararg val floats: Float
    ) : VBO() {
        override fun bind(i: Int): Int {
            var buffer: FloatBuffer? = null
            return try {
                val vboId = GL15.glGenBuffers()
                buffer = Heap.mallocFloat(floats.size)
                buffer.put(floats).flip()
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW)
                GL20.glVertexAttribPointer(i, size, GL11.GL_FLOAT, false, 0, 0)
                GL30.glEnableVertexAttribArray(i)
                vboId
            } finally {
                if (buffer != null) Heap.free(buffer)
            }
        }
    }
}