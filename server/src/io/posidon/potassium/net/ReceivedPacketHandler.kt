package io.posidon.potassium.net

import io.posidon.library.types.Vec3i
import io.posidon.potassium.Console
import io.posidon.potassium.print
import io.posidon.potassium.world.World

object ReceivedPacketHandler {
    operator fun invoke(player: Player, packet: String) { try {
        val tokens = packet.split('&')
        when (tokens[0]) {
            "mov" -> {
                val coords = tokens[1].split(',')
                player.triggerTickEvent {
                    position.set(coords[0].toFloat(), coords[1].toFloat(), coords[2].toFloat())
                }
            }
            "blockbr" -> {
                val (x, y, z) = tokens[1].split(',').let {
                    Vec3i(it[0].toInt(), it[1].toInt(), it[2].toInt())
                }
                try {
                    World.breakBlock(player, x, y, z)
                } catch (e: Exception) { e.print() }
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