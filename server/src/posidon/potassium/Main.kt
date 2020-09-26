package posidon.potassium

import posidon.potassium.net.Players
import posidon.potassium.net.Server
import posidon.potassium.world.EarthWorld
import java.io.IOException
import java.util.concurrent.TimeUnit

var running = true
inline fun loop(methods: () -> Unit) { while (running) methods() }

object Worlds {
	lateinit var earthWorld: EarthWorld
}

fun main(args: Array<String>) {
	Thread(Console()).start()
	Thread(Server()).start()
	Worlds.earthWorld = EarthWorld(7480135)
	Thread(Worlds.earthWorld).start()

	var lastTime: Long = System.nanoTime()
	val amountOfTicks = 60.0
	val ns: Double = 1000000000.0 / amountOfTicks
	var delta = 0.0
	while (running) {
		val now: Long = System.nanoTime()
		delta += (now - lastTime) / ns
		lastTime = now
		while (delta >= 1) {
			Globals.tick()
			delta--
		}
		TimeUnit.NANOSECONDS.sleep((ns - (now - lastTime)).toLong())
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