package posidon.uraniumGame.net

import posidon.uranium.gameLoop.GameLoop
import posidon.uraniumGame.net.packets.JoinPacket
import posidon.uraniumGame.net.packets.Packet
import java.io.*
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

object Client {

    private lateinit var socket: Socket
    private lateinit var output: OutputStream
    private lateinit var input: InputStream
    private lateinit var writer: OutputStreamWriter
    fun start(ip: String?, port: Int, onEnd: (Boolean) -> Unit) = thread (isDaemon = true) {
        try {
            socket = Socket(ip, port)
            output = socket.getOutputStream()
            input = socket.getInputStream()
            writer = OutputStreamWriter(output, Charsets.UTF_8)
            send(JoinPacket("leoxshn", "w04m58cyp49y59ti5ts9io3k"))
            var line: String
            do line = input.bufferedReader(Charsets.UTF_8).readLine()
            while (!line.startsWith("dict&"))
            val tokens = line.split('&')
            val defs = tokens[1].split(',')
            for (def in defs) {
                val eqI = def.indexOf('=')
                ReceivedPacketHandler.blockDictionary[def.substring(0, eqI).toInt()] = def.substring(eqI + 1)
            }
            onEnd(true)
            thread(name = "uraniumClient") {
                GameLoop.loop {
                    try { input.reader(Charsets.UTF_8).forEachLine { ReceivedPacketHandler(it) } }
                    catch (e: EOFException) { kill() }
                    catch (e: SocketException) { kill() }
                    catch (e: StreamCorruptedException) { kill() }
                    catch (e: Exception) { e.printStackTrace() }
                }
                kill()
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
        catch (e: SocketException) { kill() }
        catch (e: Exception) { e.printStackTrace() }
    }

    fun kill() {
        try {
            output.close()
            input.close()
            socket.close()
        } catch (ignore: Exception) {}
    }
}