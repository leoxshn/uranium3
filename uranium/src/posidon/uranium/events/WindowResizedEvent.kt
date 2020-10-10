package posidon.uranium.events

class WindowResizedEvent internal constructor(
    val oldWidth: Int,
    val oldHeight: Int,
    val newWidth: Int,
    val newHeight: Int
) : Event()