package posidon.uraniumGame.events

import posidon.uranium.input.events.Event

class PacketReceivedEvent(
    val packet: String,
    val tokens: List<String>
) : Event()