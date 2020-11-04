package posidon.potassium.net.packets

import posidon.library.util.Compressor

class ChatPacket(val sender: String, val message: String, val private: Boolean) : Packet("ch") {
    override fun packToString() = "${if (private) '1' else '0'}&" +
            if (private) Compressor.compressString("$sender&$message", 2048)
            else "$sender&$message"
}