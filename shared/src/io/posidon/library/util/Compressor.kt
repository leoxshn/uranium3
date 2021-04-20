package io.posidon.library.util

import java.util.*
import java.util.zip.Deflater
import java.util.zip.Inflater

object Compressor {

    fun compress(input: ByteArray, maxByteSize: Int): Pair<ByteArray, Int> {
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        val buffer = ByteArray(maxByteSize)
        val length = deflater.deflate(buffer)
        deflater.end()
        return buffer to length
    }

    fun decompress(input: ByteArray, maxByteSize: Int): Pair<ByteArray, Int> {
        val inflater = Inflater()
        inflater.setInput(input)
        val buffer = ByteArray(maxByteSize)
        val length = inflater.inflate(buffer)
        inflater.end()
        return buffer to length
    }

    inline fun compressString(input: String, maxByteSize: Int): String {
        val (buffer, length) = compress(input.toByteArray(Charsets.UTF_16), maxByteSize)
        return Base64.getEncoder().encodeToString(buffer.copyOf(length))
    }

    inline fun decompressString(input: String, maxByteSize: Int): String {
        val (buffer, length) = decompress(Base64.getDecoder().decode(input), maxByteSize)
        return String(buffer, 0, length, Charsets.UTF_16)
    }
}