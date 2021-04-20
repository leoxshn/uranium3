package io.posidon.potassium

import io.posidon.potassium.net.Player
import io.posidon.potassium.net.Players
import io.posidon.potassium.world.World
import io.posidon.uranium.net.Packet
import io.posidon.uranium.net.server.Server
import io.posidon.uranium.net.server.ServerApi
import io.posidon.uraniumPotassium.content.Block
import java.io.IOException

var running = true
inline fun loop(methods: () -> Unit) { while (running) methods() }

val Server = Server(2512)

fun Server.sendToAllPlayers(packet: Packet) {
	for (p in Players) p.send(packet)
}

fun main(args: Array<String>) {
	Thread(Console()).start()
	Server.onException = { it.print() }
	Server.start {
		val p = Player(it)
		val packet = p.waitForPacket().split("&")
		p.playerName = packet[1]
		p.id = packet[2].hashCode()
		Players.add(p)
		p.send(ServerApi.init(0f, World.getDefaultSpawnPosition().toFloat(), 0f, buildString {
			for (value in Block.values())
				append(value.ordinal).append('=').append(value.id).append(',')
			deleteCharAt(lastIndex)
		}))
		Console.beforeCmdLine {
			Console.printInfo(p.playerName!!, " joined the server")
		}
		p.start()
	}
	World.init(7480135)

	var lastTime: Long = System.nanoTime()
	var delta = 0.0
	loop {
		val now: Long = System.nanoTime()
		delta += (now - lastTime) / 1000000000.0
		while (delta >= 0.001) {
			Globals.tick(delta)
			delta = 0.0
		}
		lastTime = now
	}
}

fun stop() {
	running = false
	Console.println("Stopping server...")
	for (player in Players) player.kick()
	try { Server.close() }
	catch (e: IOException) { e.print() }
}

fun Throwable.print() = Console.beforeCmdLine {
	print(Console.colors.RED)
	printStackTrace()
	print(Console.colors.RESET)
}