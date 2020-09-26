package posidon.potassium.net

import posidon.potassium.Console
import kotlin.math.cos
import kotlin.math.sin

object ReceivedPacketHandler {
    operator fun invoke(player: Player, packet: String) { try {
        val tokens = packet.split('&');
        when (tokens[0]) {
            "mov" -> {
                val coords = tokens[1].split(',')
                player.triggerTickEvent {
                    position.set(coords[0].toFloat(), coords[1].toFloat(), coords[2].toFloat())
                    Console.beforeCmdLine {
                        Console.printInfo(player.playerName!!, " -> " + player.position.x + " / " + player.position.y + " / " + player.position.z)
                    }
                }
            }
            else -> Console.beforeCmdLine {
                Console.printProblem(player.name, " sent an unknown packet: $packet")
            }
        }
    } catch (e: Exception) {
        Console.beforeCmdLine {
            Console.printProblem(player.name, " sent an unknown packet: $packet")
        }
    }}
}