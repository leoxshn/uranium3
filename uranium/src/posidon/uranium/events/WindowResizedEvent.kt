package posidon.uranium.events

class WindowResizedEvent internal constructor(
    override val millis: Long,
    val oldWidth: Int,
    val oldHeight: Int,
    val newWidth: Int,
    val newHeight: Int
) : Event()