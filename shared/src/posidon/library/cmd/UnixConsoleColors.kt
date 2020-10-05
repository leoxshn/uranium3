package posidon.library.cmd

class UnixConsoleColors : ConsoleColors() {
    override val RESET = "\u001b[0m"

    override val RED = "\u001b[0;31m"
    override val GREEN = "\u001b[0;32m"
    override val YELLOW = "\u001b[0;33m"
    override val BLUE = "\u001b[0;34m"
    override val PURPLE = "\u001b[0;35m"
    override val CYAN = "\u001b[0;36m"
    override val RED_BRIGHT = "\u001b[0;91m"
    override val GREEN_BRIGHT = "\u001b[0;92m"
    override val YELLOW_BRIGHT = "\u001b[0;93m"
    override val BLUE_BRIGHT = "\u001b[0;94m"
    override val PURPLE_BRIGHT = "\u001b[0;95m"
    override val CYAN_BRIGHT = "\u001b[0;96m"

    override val RED_BOLD = "\u001b[1;31m"
    override val GREEN_BOLD = "\u001b[1;32m"
    override val YELLOW_BOLD = "\u001b[1;33m"
    override val BLUE_BOLD = "\u001b[1;34m"
    override val PURPLE_BOLD = "\u001b[1;35m"
    override val CYAN_BOLD = "\u001b[1;36m"
    override val RED_BOLD_BRIGHT = "\u001b[1;91m"
    override val GREEN_BOLD_BRIGHT = "\u001b[1;92m"
    override val YELLOW_BOLD_BRIGHT = "\u001b[1;93m"
    override val BLUE_BOLD_BRIGHT = "\u001b[1;94m"
    override val PURPLE_BOLD_BRIGHT = "\u001b[1;95m"
    override val CYAN_BOLD_BRIGHT = "\u001b[1;96m"
}