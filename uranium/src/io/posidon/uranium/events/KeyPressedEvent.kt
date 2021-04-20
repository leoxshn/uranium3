package io.posidon.uranium.events

import io.posidon.uranium.graphics.Window

class KeyPressedEvent internal constructor(
    override val millis: Long,
    val window: Window,
    val key: Int,
    val action: Int
) : Event()