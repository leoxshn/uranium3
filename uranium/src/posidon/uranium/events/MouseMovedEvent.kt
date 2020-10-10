package posidon.uranium.events

import posidon.library.types.Vec2f

class MouseMovedEvent internal constructor(
    val cursorPosition: Vec2f,
    val cursorMovement: Vec2f
) : Event()