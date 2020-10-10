package posidon.uranium.events

class KeyPressedEvent internal constructor(
    val key: Int,
    val action: Int
) : Event()