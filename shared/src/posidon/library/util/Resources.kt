package posidon.library.util

import java.io.BufferedReader
import java.io.InputStreamReader

object Resources {
    fun loadAsString(path: String): String {
        val result = StringBuilder()
        try {
            BufferedReader(InputStreamReader(Resources::class.java.getResourceAsStream(path))).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) result.append(line).append("\n")
            }
        } catch (e: Exception) {
            System.err.println("Couldn't find the file $path")
            e.printStackTrace()
        }
        return result.toString()
    }
}