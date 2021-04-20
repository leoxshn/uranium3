package io.posidon.uraniumGame

import io.posidon.uranium.Uranium
import io.posidon.uranium.events.Event

class WorldEnvironment {

    companion object {
        private const val MAX_TIME = 30.0
    }

    private var time = MAX_TIME / 2.0
    var timeSpeed = 1

    fun update(delta: Double) {
        time += timeSpeed * delta
        time %= MAX_TIME
    }

    fun onEvent(event: Event) {
        if (event is PacketReceivedEvent) {
            when (event.tokens[0]) {
                "time" -> time = event.tokens[1].toDouble()
                "playerInfo" -> {
                    for (token in event.tokens) if (token.startsWith("time")) {
                        time = token.substring(6).toDouble()
                    }
                }
                "" -> Uranium.end()
            }
        }
    }
}