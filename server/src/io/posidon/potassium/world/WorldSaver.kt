package io.posidon.potassium.world

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class WorldSaver(
    val folderName: String
) {

    val folder = File(folderName)

    init {

        folder.mkdir()
    }

    fun saveChunk(x: Int, y: Int, z: Int, chunk: Chunk) {
        FileOutputStream(File(getChunkFolder(x, y, z).also { it.mkdirs() }, BLOCK_DATA_FILE_NAME)).use {
            it.write(chunk.getSaveBytes())
        }
    }

    fun hasChunk(x: Int, y: Int, z: Int): Boolean {
        return getChunkFolder(x, y, z).exists()
    }

    fun loadChunk(x: Int, y: Int, z: Int): Chunk? {
        return try {
            FileInputStream(File(getChunkFolder(x, y, z).also { it.mkdirs() }, BLOCK_DATA_FILE_NAME)).use {
                Chunk.readFromInputStream(it)
            }
        } catch (e: IOException) {
            null
        }
    }

    inline fun getChunkFolder(x: Int, y: Int, z: Int): File {
        return File(folder, generateChunkSaveName(x, y, z))
    }

    inline fun generateChunkSaveName(x: Int, y: Int, z: Int): String {
        return "x${x.toString(16)}y${y.toString(16)}z${z.toString(16)}"
    }

    companion object {
        const val BLOCK_DATA_FILE_NAME = "blocks"
    }
}
