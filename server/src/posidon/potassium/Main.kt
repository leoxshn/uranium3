package posidon.potassium

import posidon.potassium.net.Players
import posidon.potassium.net.Server
import posidon.potassium.world.EarthWorld
import posidon.potassium.world.Worlds
import java.io.IOException
import java.util.concurrent.TimeUnit

var running = true
inline fun loop(methods: () -> Unit) { while (running) methods() }

fun main(args: Array<String>) {
	Thread(Console()).start()
	Thread(Server()).start()
	Worlds.start(EarthWorld(7480135))

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
	try { Server.socket.close() }
	catch (e: IOException) { e.print() }
}

fun Throwable.print() = Console.beforeCmdLine {
	print(Console.colors.RED)
	printStackTrace()
	print(Console.colors.RESET)
}