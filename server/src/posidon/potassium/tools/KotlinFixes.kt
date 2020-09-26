package posidon.potassium.tools

object KotlinFixes {
    inline infix fun Byte.eq(int: Int) = this == int.toByte()
    inline infix fun Any.neq(any: Any) = this != any
}