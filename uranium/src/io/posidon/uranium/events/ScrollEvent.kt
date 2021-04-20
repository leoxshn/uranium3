package io.posidon.uranium.events

import io.posidon.uranium.graphics.Window

class ScrollEvent internal constructor(
    override val millis: Long,
    val window: Window,
    val x: Double,
    val y: Double
) : Event()