package posidon.uranium.nodes.spatial

import posidon.library.types.Matrix4f
import posidon.library.types.Vec2f
import posidon.library.types.Vec3i
import posidon.uranium.graphics.Window
import kotlin.math.cos
import kotlin.math.sin

open class Camera(
    name: String
) : Spatial(name) {

    var rotation = Vec2f(0f, 0f)
    var viewMatrix: Matrix4f = Matrix4f.view(globalTransform.position, rotation)

    fun isPositionInFov(position: Vec3i): Boolean {
        val posRelToCam = position - this.position.toVec3i()
        val rotY = Math.toRadians((rotation.y - 180).toDouble())
        val cosRY = cos(rotY)
        val sinRY = sin(rotY)
        val rotX = Math.toRadians(rotation.x.toDouble())
        val cosRX = cos(rotX)
        val sinRX = sin(rotX)
        val x = (posRelToCam.x * cosRY - posRelToCam.z * sinRY) * cosRX + posRelToCam.y * sinRX
        val z = (posRelToCam.z * cosRY + posRelToCam.x * sinRY) * cosRX + posRelToCam.y * sinRX
        val y = posRelToCam.y * cosRX - z * sinRX
        val maxXOffset: Double = z * Window.width / Window.height
        val maxYOffset = z * cosRX + posRelToCam.y * sinRX
        return z > 0 && x < maxXOffset && x > -maxXOffset && y < maxYOffset && y > -maxYOffset
    }
}