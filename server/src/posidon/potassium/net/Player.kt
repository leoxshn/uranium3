package posidon.potassium.net

import posidon.library.types.Vec3f
import posidon.library.types.Vec3i
import posidon.potassium.Console
import posidon.potassium.net.packets.ChunkPacket
import posidon.potassium.net.packets.Packet
import posidon.potassium.print
import posidon.potassium.world.Chunk
import posidon.potassium.world.World
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class Player(
    private val socket: Socket
) : Thread(socket.inetAddress.hostAddress) {

    private val output: OutputStream = socket.getOutputStream()
    private val input: InputStream = socket.getInputStream()
    private val tickEventQueue = ConcurrentLinkedQueue<Player.() -> Unit>()

    val sentChunks = ArrayList<Vec3i>()

    fun triggerTickEvent(it: Player.() -> Unit) = tickEventQueue.add(it)

    private var running = true

    var playerName: String? = null
    var id = 0

    val position = Vec3f(0f, 0f, 0f)

    var moveSpeed = 0.5f
    var jumpHeight = 0.5f

    private val writer = OutputStreamWriter(output, Charsets.UTF_8)

    fun sendChunk(chunkPos: Vec3i, chunk: Chunk) {
        send(ChunkPacket(chunk, chunkPos))
        sentChunks.add(chunkPos)
    }

    fun send(packet: Packet) {
        if (running) try {
            writer.write(packet.toString())
            writer.write(0x0a)
            writer.flush()
        }
        catch (e: SocketException) { disconnect() }
        catch (e: Exception) { e.print() }
    }

    fun waitForPacket(): String {
        var tmp = ""
        do try { tmp = input.bufferedReader(Charsets.UTF_8).readLine() } catch (e: Exception) { e.print() }
        while (!tmp.startsWith("join&") && running)
        return tmp
    }

    var world: World? = null
        set(value) {
            field?.players?.remove(this)
            sentChunks.clear()
            field = value
            field?.players?.add(this)
        }

    override fun run() {
        thread {
            var lastTime: Long = System.nanoTime()
            var delta = 0.0

            while (running) {
                val now: Long = System.nanoTime()
                delta += (now - lastTime) / 1000000000.0
                lastTime = now
                if (delta >= 0.01) {
                    tick()
                    delta--
                }
            }
        }

        while (running) {
            var string: String? = ""
            try {
                string = input.bufferedReader(Charsets.UTF_8).readLine()
            }
            catch (e: SocketException) {}
            catch (e: Exception) { e.print() }
            if (string.isNullOrEmpty() && running) {
                disconnect()
            }
            else if (string != null) ReceivedPacketHandler(this, string)
        }
    }

    private inline fun tick() {
        if (tickEventQueue.size > 3) Console.beforeCmdLine {
            Console.printProblem(playerName!!, " is sending packets to fast! (${tickEventQueue.size} per tick)")
        }
        tickEventQueue.removeIf { it(); true }

        world?.sendChunks(this)
    }

    fun disconnect() {
        destroy()
        Console.beforeCmdLine { Console.printInfo(playerName!!, " left the server") }
    }

    fun kick() {
        destroy()
        Console.printInfo(playerName!!, " left the server")
    }

    fun destroy() {
        running = false
        try {
            output.close()
            input.close()
            socket.close()
        } catch (ignore: Exception) {}
        Players.remove(id)
        world = null
    }
}