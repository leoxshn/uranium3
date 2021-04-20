package io.posidon.uraniumGame

import io.posidon.uranium.events.Event

class PacketReceivedEvent(
    override val millis: Long,
    val packet: String,
    val tokens: List<String>
) : Event()