package posidon.uranium.events

import posidon.library.types.Vec2f

class MouseButtonPressedEvent internal constructor(
    override val millis: Long,
    val button: Int,
    val action: Int,
    val position: Vec2f
) : Event()