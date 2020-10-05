package posidon.potassium

import posidon.library.cmd.NoConsoleColors
import posidon.library.cmd.UnixConsoleColors
import posidon.potassium.net.Chat
import posidon.potassium.net.Players
import posidon.potassium.net.Server
import posidon.potassium.net.packets.TimePacket
import java.io.File
import kotlin.concurrent.thread

class Console : Runnable {

    companion object {
        val colors = if (File.listRoots().size == 1 && File.listRoots()[0].path == "/") UnixConsoleColors() else NoConsoleColors()
        val errHighlightColor = colors.YELLOW_BOLD_BRIGHT
        val errColor = colors.RED
        const val prefix = "> "

        inline fun println(string: String) = kotlin.io.println(string + colors.RESET)
        inline fun print(string: String) = kotlin.io.print(string + colors.RESET)
        inline fun printProblem(highlight: String, otherText: String) = println(errHighlightColor + highlight + errColor + otherText)
        inline fun printInfo(highlight: String, otherText: String) = println(colors.CYAN_BOLD_BRIGHT + highlight + colors.BLUE + otherText)

        inline fun backspace(characters: Int) {
            for (i in 0 until characters) print('\b')
        }

        inline fun beforeCmdLine(stuff: () -> Unit) {
            backspace(prefix.length)
            stuff()
            print(colors.BLUE_BOLD + prefix)
        }
    }

    override fun run() {
        loop {
            print(colors.BLUE_BOLD + prefix)
            val line = readLine()!!
            val cmd = line.split(' ')
            when (cmd[0]) {
                "", " " -> {}
                "stop" -> stop()
                "ip" -> {
                    thread (isDaemon = true) {
                        val ip = Server.extIP
                        beforeCmdLine {
                            println(ip?.let { colors.PURPLE_BOLD + "ip" + colors.PURPLE + " -> " + colors.RESET + it } ?: colors.RED + "error: couldn't get external ip")
                        }
                    }
                }
                "kick" -> {
                    if (cmd.size == 1) println(errColor + "kick who?")
                    else for (i in 1 until cmd.size) {
                        Players[cmd[i]]?.kick()
                            ?: println(errColor + "there's no player called " + errHighlightColor + cmd[i] + errColor + " on the server")
                    }
                }
                "players" -> {
                    if (Players.isEmpty) println("There are no players online")
                    else for (player in Players) println(player.playerName)
                }
                "time" -> {
                    if (cmd.size == 1) println(colors.PURPLE_BOLD + "time" + colors.PURPLE + " = " + colors.RESET + Globals.time)
                    else {
                        when (cmd[1]) {
                            "=", "set" -> {
                                if (cmd.size == 3) {
                                    cmd[2].toDoubleOrNull()?.let {
                                        Globals.time = it
                                        Server.sendToAllPlayers(TimePacket())
                                        println("Time set to ${Globals.time}")
                                    } ?: println(errColor + cmd[2] + " isn't a number!")
                                } else println(errColor + "set time to what?")
                            }
                            "+=", "add" -> {
                                if (cmd.size == 3) {
                                    cmd[2].toDoubleOrNull()?.let {
                                        Globals.time += it
                                        Server.sendToAllPlayers(TimePacket())
                                        println("Time set to ${Globals.time}")
                                    } ?: println(errColor + cmd[2] + " isn't a number!")
                                } else println(errColor + "add what to time?")
                            }
                            else -> { printProblem(cmd[1], " isn't a valid parameter!") }
                        }
                    }
                }
                "ch" -> {
                    Chat.post("[server]", line.substring(3))
                    printInfo("[server]:", ' ' + line.substring(3))
                }
                else -> printProblem(cmd[0], " isn't a valid command!")
            }
        }
    }
}