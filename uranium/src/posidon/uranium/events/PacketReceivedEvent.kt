package posidon.uranium.events

class PacketReceivedEvent(
    override val millis: Long,
    val packet: String,
    val tokens: List<String>
) : Event()