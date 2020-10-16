package posidon.uranium.net

import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.gameLoop.GameLoop
import posidon.uranium.nodes.Scene
import java.io.*
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

object Client {

    private lateinit var socket: Socket
    private lateinit var output: OutputStream
    private lateinit var input: InputStream
    private lateinit var writer: OutputStreamWriter

    fun start(ip: String, port: Int, onEnd: (Boolean) -> Unit) = thread (isDaemon = true) {
        try {
            socket = Socket(ip, port)
            output = socket.getOutputStream()
            input = socket.getInputStream()
            writer = OutputStreamWriter(output, Charsets.UTF_8)

            onEnd(true)

            thread(name = "uraniumClient") {
                GameLoop.loop {
                    try { input.reader(Charsets.UTF_8).forEachLine {
                        val tokens = it.split('&')
                        Scene.passEvent(PacketReceivedEvent(System.currentTimeMillis(), it, tokens))
                    }}
                    catch (e: EOFException) { stop() }
                    catch (e: SocketException) { stop() }
                    catch (e: StreamCorruptedException) { stop() }
                    catch (e: Exception) { e.printStackTrace() }
                }
                stop()
            }
        } catch (e: Exception) {
            System.err.println("[CONNECTION ERROR]: Can't connect to potassium server")
            onEnd(false)
        }
    }

    fun send(packet: Packet) {
        try {
            writer.write(packet.toString())
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
    }

    fun waitForPacket(name: String): String {
        var line: String
        do line = input.bufferedReader(Charsets.UTF_8).readLine()
        while (!line.startsWith("$name&"))
        return line
    }
}