package posidon.uranium.events

class ScrollEvent internal constructor(
    override val millis: Long,
    val x: Double,
    val y: Double
) : Event()