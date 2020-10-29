package posidon.uraniumGame.ui

import posidon.library.types.Vec2f
import posidon.uranium.nodes.ui.text.MonospaceFont

class Font : MonospaceFont("res/textures/ui/font.png") {

    override val glyphWidth = 5
    override val glyphHeight = 7

    override fun getPosition(char: Char): Vec2f = when (char) {
        'A' -> Vec2f(0f, 0f)
        'B' -> Vec2f(1f, 0f)
        'C' -> Vec2f(2f, 0f)
        'D' -> Vec2f(3f, 0f)
        'E' -> Vec2f(4f, 0f)
        'F' -> Vec2f(5f, 0f)
        'G' -> Vec2f(6f, 0f)
        'H' -> Vec2f(7f, 0f)
        'I' -> Vec2f(8f, 0f)
        'J' -> Vec2f(9f, 0f)
        'K' -> Vec2f(10f, 0f)
        'L' -> Vec2f(11f, 0f)
        'M' -> Vec2f(12f, 0f)
        'N' -> Vec2f(13f, 0f)
        'O' -> Vec2f(14f, 0f)
        'P' -> Vec2f(15f, 0f)
        'Q' -> Vec2f(16f, 0f)
        'R' -> Vec2f(17f, 0f)
        'S' -> Vec2f(18f, 0f)
        'T' -> Vec2f(19f, 0f)
        'U' -> Vec2f(20f, 0f)
        'V' -> Vec2f(21f, 0f)
        'W' -> Vec2f(22f, 0f)
        'X' -> Vec2f(23f, 0f)
        'Y' -> Vec2f(24f, 0f)
        'Z' -> Vec2f(25f, 0f)
        'Ñ' -> Vec2f(26f, 0f)
        'Ç' -> Vec2f(27f, 0f)

        'a' -> Vec2f(0f, 1f)
        'b' -> Vec2f(1f, 1f)
        'c' -> Vec2f(2f, 1f)
        'd' -> Vec2f(3f, 1f)
        'e' -> Vec2f(4f, 1f)
        'f' -> Vec2f(5f, 1f)
        'g' -> Vec2f(6f, 1f)
        'h' -> Vec2f(7f, 1f)
        'i' -> Vec2f(8f, 1f)
        'j' -> Vec2f(9f, 1f)
        'k' -> Vec2f(10f, 1f)
        'l' -> Vec2f(11f, 1f)
        'm' -> Vec2f(12f, 1f)
        'n' -> Vec2f(13f, 1f)
        'o' -> Vec2f(14f, 1f)
        'p' -> Vec2f(15f, 1f)
        'q' -> Vec2f(16f, 1f)
        'r' -> Vec2f(17f, 1f)
        's' -> Vec2f(18f, 1f)
        't' -> Vec2f(19f, 1f)
        'u' -> Vec2f(20f, 1f)
        'v' -> Vec2f(21f, 1f)
        'w' -> Vec2f(22f, 1f)
        'x' -> Vec2f(23f, 1f)
        'y' -> Vec2f(24f, 1f)
        'z' -> Vec2f(25f, 1f)
        'ñ' -> Vec2f(26f, 1f)
        'ç' -> Vec2f(27f, 1f)

        '1' -> Vec2f(0f, 2f)
        '2' -> Vec2f(1f, 2f)
        '3' -> Vec2f(2f, 2f)
        '4' -> Vec2f(3f, 2f)
        '5' -> Vec2f(4f, 2f)
        '6' -> Vec2f(5f, 2f)
        '7' -> Vec2f(6f, 2f)
        '8' -> Vec2f(7f, 2f)
        '9' -> Vec2f(8f, 2f)
        '0' -> Vec2f(9f, 2f)

        '.' -> Vec2f(10f, 2f)
        ',' -> Vec2f(11f, 2f)
        ':' -> Vec2f(12f, 2f)
        ';' -> Vec2f(13f, 2f)
        '!' -> Vec2f(14f, 2f)
        '?' -> Vec2f(15f, 2f)
        '¡' -> Vec2f(16f, 2f)
        '¿' -> Vec2f(17f, 2f)
        '*' -> Vec2f(18f, 2f)
        '+' -> Vec2f(19f, 2f)
        '-' -> Vec2f(20f, 2f)
        '_' -> Vec2f(21f, 2f)
        '(' -> Vec2f(22f, 2f)
        ')' -> Vec2f(22f, 2f)
        '{' -> Vec2f(23f, 2f)
        '}' -> Vec2f(23f, 2f)
        '[' -> Vec2f(24f, 2f)
        ']' -> Vec2f(24f, 2f)
        '<' -> Vec2f(25f, 2f)
        '>' -> Vec2f(25f, 2f)

        '|' -> Vec2f(0f, 3f)
        '@' -> Vec2f(1f, 3f)
        '#' -> Vec2f(2f, 3f)
        '$' -> Vec2f(3f, 3f)
        '%' -> Vec2f(4f, 3f)
        '&' -> Vec2f(5f, 3f)
        '/' -> Vec2f(6f, 3f)
        '=' -> Vec2f(7f, 3f)
        '\'' -> Vec2f(8f, 3f)
        '"' -> Vec2f(9f, 3f)

        ' ' -> Vec2f(-1f, -1f)

        else -> Vec2f(34f, 1f)
    }

    override fun isFlipped(char: Char) = when (char) {
        ')', '}', ']', '>' -> true
        else -> false
    }
}