package posidon.uranium.events

class KeyPressedEvent internal constructor(
    override val millis: Long,
    val key: Int,
    val action: Int
) : Event()