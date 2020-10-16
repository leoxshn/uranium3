package posidon.uranium.net

abstract class Packet(val name: String) {
    protected abstract fun packToString(): String
    override fun toString(): String = name + '&' + packToString()
}