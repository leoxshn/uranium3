package posidon.uraniumGame.events

import posidon.uranium.events.Event

class PacketReceivedEvent(
    override val millis: Long,
    val packet: String,
    val tokens: List<String>
) : Event()