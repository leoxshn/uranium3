package io.posidon.uranium.events

import io.posidon.library.types.Vec2f
import io.posidon.uranium.graphics.Window

class MouseMovedEvent internal constructor(
    override val millis: Long,
    val window: Window,
    val cursorPosition: Vec2f,
    val cursorMovement: Vec2f
) : Event()