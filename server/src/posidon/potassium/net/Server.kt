package posidon.potassium.net

import posidon.potassium.Console
import posidon.potassium.loop
import posidon.potassium.net.packets.InitPacket
import posidon.potassium.net.packets.Packet
import posidon.potassium.print
import posidon.potassium.world.Worlds
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.SocketException
import java.net.URL
import kotlin.concurrent.thread

object Server {

    private const val port = 2512

    val socket = ServerSocket(port)

    fun sendToAllPlayers(packet: Packet) {
        for (player in Players)
            player.send(packet)
    }

    fun start() = thread (isDaemon = true) {
        try {
            Console.beforeCmdLine {
                Console.println("Starting server...")
            }
            loop {
                try {
                    val p = Player(socket.accept())
                    val packet = p.waitForPacket().split("&")
                    p.playerName = packet[1]
                    p.id = packet[2].hashCode()
                    Players.add(p)
                    p.send(InitPacket(0f, 0f, 0f))
                    p.world = Worlds["terra"]
                    Console.beforeCmdLine {
                        Console.printInfo(p.playerName!!, " joined the server")
                    }
                    p.start()
                }
                catch (e: SocketException) {}
                catch (e: Exception) { e.print() }
            }
        } catch (e: IOException) { e.print() }
    }

    val extIP: String?
        get() {
            var out: String? = null
            try {
                val ipUrl = URL("http://checkip.amazonaws.com")
                var input: BufferedReader? = null
                try {
                    input = BufferedReader(InputStreamReader(ipUrl.openStream()))
                    out = input.readLine()
                } catch (e: Exception) {}
                input?.close()
            } catch (e: Exception) {}
            return out
        }
}