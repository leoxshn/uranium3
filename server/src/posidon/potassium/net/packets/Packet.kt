package posidon.potassium.net.packets

abstract class Packet(private val name: String) {
    protected abstract fun packToString(): String
    override fun toString(): String = name + '&' + packToString()
}