package io.posidon.uranium.events

import io.posidon.uranium.graphics.Window

class WindowResizedEvent internal constructor(
    override val millis: Long,
    val window: Window,
    val oldWidth: Int,
    val oldHeight: Int,
    val newWidth: Int,
    val newHeight: Int
) : Event()