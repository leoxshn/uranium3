package io.posidon.uranium.net.server

import io.posidon.library.util.Compressor
import io.posidon.uranium.net.Packet

object ServerApi {

    fun init(x: Float, y: Float, z: Float, blockDictionary: String): Packet = "init&$blockDictionary&$x&$y&$z".toCharArray()

    fun block(x: Int, y: Int, z: Int, id: Int): Packet = "block&$x,$y,$z&$id".toCharArray()

    fun time(time: Double): Packet = "time&$time".toCharArray()

    fun chat(sender: String, message: String, private: Boolean): Packet = ("ch&${if (private) '1' else '0'}&" +
        if (private) Compressor.compressString("$sender&$message", 2048)
        else "$sender&$message").toCharArray()

    fun chunk(x: Int, y: Int, z: Int, dataString: String?): Packet = (if (dataString == null) "chunk&$x&$y&$z&" else "chunk&$x&$y&$z&$dataString").toCharArray()
}