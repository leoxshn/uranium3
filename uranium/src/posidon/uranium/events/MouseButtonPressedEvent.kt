package posidon.uranium.events

class MouseButtonPressedEvent internal constructor(
    override val millis: Long,
    val button: Int,
    val action: Int
) : Event()