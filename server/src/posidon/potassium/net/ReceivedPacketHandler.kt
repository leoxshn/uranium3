package posidon.potassium.net

import posidon.library.types.Vec3i
import posidon.potassium.Console
import posidon.potassium.net.packets.BlockPacket
import posidon.potassium.print
import posidon.potassium.world.Chunk

object ReceivedPacketHandler {
    operator fun invoke(player: Player, packet: String) { try {
        val tokens = packet.split('&')
        when (tokens[0]) {
            "mov" -> {
                val coords = tokens[1].split(',')
                player.triggerTickEvent {
                    position.set(coords[0].toFloat(), coords[1].toFloat(), coords[2].toFloat())
                    /*Console.beforeCmdLine {
                        Console.printInfo(player.playerName!!, " -> " + player.position.x + " / " + player.position.y + " / " + player.position.z)
                    }*/
                }
            }
            "blockbr" -> {
                val coords = tokens[1].split(',').let {
                    Vec3i(it[0].toInt(), it[1].toInt(), it[2].toInt())
                }
                player.world?.setBlock(coords, null)
                player.send(BlockPacket(coords, -1))
            }
            else -> Console.beforeCmdLine {
                Console.printProblem(player.name, " sent an unknown packet: $packet")
            }
        }
    } catch (e: Exception) {
        Console.beforeCmdLine {
            Console.printProblem(player.name, " sent an packet that couldn't be processed: $packet")
            e.print()
        }
    }}
}