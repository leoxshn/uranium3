package posidon.potassium.net

import posidon.library.types.Vec3f
import posidon.potassium.Console
import posidon.potassium.world.Worlds
import posidon.potassium.net.packets.BlockDictionaryPacket
import posidon.potassium.net.packets.Packet
import posidon.potassium.print
import posidon.library.types.Vec3i
import posidon.potassium.net.packets.ChunkPacket
import posidon.potassium.world.Chunk
import posidon.potassium.world.World
import java.io.*
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.math.roundToInt

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

    fun send(chunk: Chunk) {
        send(ChunkPacket(chunk))
        sentChunks.add(chunk.position)
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

    var world: World? = null
        set(value) {
            field?.players?.remove(this)
            sentChunks.clear()
            field = value
            field?.players?.add(this)
        }

    override fun run() {
        try {
            var tmp = ""
            do try { tmp = input.bufferedReader(Charsets.UTF_8).readLine() }
            catch (e: Exception) { e.print() }
            while (!tmp.startsWith("join&") && running)
            send(BlockDictionaryPacket())
            val packet = tmp.split("&")
            playerName = packet[1]
            id = packet[2].hashCode()
            Players.add(this)
            world = Worlds["terra"]
            Console.beforeCmdLine {
                Console.printInfo(playerName!!, " joined the server")
            }
        } catch (e: IOException) { e.print() }

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

        val w = world
        if (w != null) {
            val loadChunks = w.loadDistance / Chunk.SIZE
            val xx = (position.x / Chunk.SIZE).roundToInt()
            val yy = (position.y / Chunk.SIZE).roundToInt()
            val zz = (position.z / Chunk.SIZE).roundToInt()
            for (x in -loadChunks..loadChunks)
                for (y in -loadChunks..loadChunks)
                    for (z in -loadChunks..loadChunks) {
                        val chunkPos = Vec3i(xx + x, yy + y, zz + z)
                        if (!sentChunks.contains(chunkPos) && chunkPos.y >= -7 && chunkPos.y <= 7) {
                            send(w.getChunk(chunkPos))
                        }
                    }
        }
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