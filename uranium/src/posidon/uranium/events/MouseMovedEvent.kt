package posidon.uranium.events

import posidon.library.types.Vec2f

class MouseMovedEvent internal constructor(
    override val millis: Long,
    val cursorPosition: Vec2f,
    val cursorMovement: Vec2f
) : Event()