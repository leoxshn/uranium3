package io.posidon.library.util

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

operator fun ByteBuffer.set(i: Int, v: Byte) = put(i, v)
operator fun IntBuffer.set(i: Int, v: Int) = put(i, v)
operator fun FloatBuffer.set(i: Int, v: Float) = put(i, v)
operator fun ShortBuffer.set(i: Int, v: Short) = put(i, v)