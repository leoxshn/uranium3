package posidon.library.util

inline fun String.newLineEscape() = replace("\\", "\\\\").replace("\n", "\\n")
inline fun String.newLineUnescape() = replace(Regex("(?<=(?<!\\)\\(\\\\))*\\n"), "\n").replace("\\\\", "\\")

inline fun jumpLoop(topBound: Int, fn: (i: Int) -> Unit) {
    var i = 0
    var a = 0
    do {
        i += a
        a = 1 - a
        i *= -1
        fn(i)
    } while (i != topBound)
}