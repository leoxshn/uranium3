package posidon.uranium.events

class MouseButtonPressedEvent internal constructor(
    val button: Int,
    val action: Int
) : Event()