package posidon.uranium.engine.input.events

import posidon.library.types.Vec2f

class MouseMovedEvent(
    val cursorPosition: Vec2f,
    val cursorMovement: Vec2f
) : Event()