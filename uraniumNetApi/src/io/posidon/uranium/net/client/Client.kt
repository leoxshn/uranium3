package io.posidon.uranium.net.client

import java.io.*
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

class Client(
    val ip: String,
    val port: Int
) {

    private lateinit var socket: Socket
    private lateinit var output: OutputStream
    private lateinit var input: InputStream
    private lateinit var writer: Writer

    var onResult: (Boolean) -> Unit = {}
    var onClose: () -> Unit = {}

    fun startAsync(isDaemon: Boolean = true, onPacketReceived: (String) -> Unit): Thread = thread (isDaemon = isDaemon) {
        start(onPacketReceived)
    }

    fun start(onPacketReceived: (String) -> Unit) {
        try {
            socket = Socket(ip, port)
            output = socket.getOutputStream()
            input = socket.getInputStream()
            writer = OutputStreamWriter(output, Charsets.UTF_8).buffered()

            onResult(true)

            try { input.reader(Charsets.UTF_8).forEachLine {
                onPacketReceived(it)
            }}
            catch (e: EOFException) { stop() }
            catch (e: SocketException) { stop() }
            catch (e: StreamCorruptedException) { stop() }
            catch (e: Exception) { e.printStackTrace() }
            stop()
        } catch (e: Exception) {
            System.err.println("[CONNECTION ERROR]: Can't connect to potassium server")
            onResult(false)
        }
    }

    fun send(charArray: CharArray) {
        try {
            writer.write(charArray)
            writer.write(0x0a)
            writer.flush()
        }
        catch (e: SocketException) { stop() }
        catch (e: Exception) { e.printStackTrace() }
    }

    fun stop() {
        try {
            output.close()
            input.close()
            socket.close()
        } catch (ignore: Exception) {}
        onClose()
    }

    fun waitForPacket(name: String): String {
        var line: String
        do line = input.bufferedReader(Charsets.UTF_8).readLine()
        while (!line.startsWith("$name&"))
        return line
    }
}