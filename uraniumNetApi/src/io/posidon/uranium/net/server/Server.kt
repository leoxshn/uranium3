package io.posidon.uranium.net.server

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URL
import kotlin.concurrent.thread

class Server(val port: Int) {

    private val socket = ServerSocket(port)

    var onException: (Exception) -> Unit = { it.printStackTrace() }

    fun start(onSocketAccepted: (Socket) -> Unit) = thread (isDaemon = true) {
        try {
            while (true) onSocketAccepted(socket.accept())
        }
        catch (e: SocketException) {}
        catch (e: Exception) { onException(e) }
    }

    fun close() {
        socket.close()
    }

    fun getExtIP(): String? {
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