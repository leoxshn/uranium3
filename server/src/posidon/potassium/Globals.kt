package posidon.potassium

object Globals {

    private const val MAX_TIME = 600.0

    var time = MAX_TIME / 2.0
    var timeSpeed = 1

    fun tick(delta: Double) {
        time += timeSpeed * delta
        time %= MAX_TIME
    }
}