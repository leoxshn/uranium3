package io.posidon.uranium.events

import io.posidon.library.types.Vec2f
import io.posidon.uranium.graphics.Window

class MouseButtonPressedEvent internal constructor(
    override val millis: Long,
    val window: Window,
    val button: Int,
    val action: Int,
    val position: Vec2f
) : Event()