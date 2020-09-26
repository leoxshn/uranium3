package posidon.potassium

object Globals {

    var time = 0.0
    var timeSpeed = 1

    private const val MAX_TIME = 24000

    fun tick() {
        time = if (time < MAX_TIME) time + timeSpeed else 0.0
    }
}