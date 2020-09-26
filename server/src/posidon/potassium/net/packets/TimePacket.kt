package posidon.potassium.net.packets

import posidon.potassium.Globals

class TimePacket : Packet("time") { override fun packToString() = Globals.time.toString() }