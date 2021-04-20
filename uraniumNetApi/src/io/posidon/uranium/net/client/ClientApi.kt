package io.posidon.uranium.net.client

import io.posidon.library.types.Vec3f
import io.posidon.library.types.Vec3i
import io.posidon.uranium.net.Packet

object ClientApi {

    fun breakBlock(position: Vec3i): Packet = "blockbr&${position.x},${position.y},${position.z}".toCharArray()

    fun goto(position: Vec3f): Packet = "mov&${position.x},${position.y},${position.z}".toCharArray()
}