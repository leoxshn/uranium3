package posidon.uranium.engine.input.events

class PacketReceivedEvent(
    val packet: String,
    val tokens: List<String>
) : Event()