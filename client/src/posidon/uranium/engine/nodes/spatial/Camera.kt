package posidon.uranium.engine.nodes.spatial

import org.lwjgl.glfw.GLFW
import posidon.uranium.net.Client
import posidon.uranium.engine.input.Input
import posidon.uranium.engine.Window
import posidon.uranium.net.packets.MovPacket
import posidon.library.types.Matrix4f
import posidon.library.types.Vec2f
import posidon.library.types.Vec3i
import posidon.library.util.Compressor
import posidon.library.util.newLineUnescape
import posidon.uranium.engine.graphics.Renderer
import posidon.uranium.engine.input.events.Event
import posidon.uranium.engine.input.events.MouseMovedEvent
import posidon.uranium.engine.input.events.PacketReceivedEvent
import posidon.uranium.engine.nodes.RootNode
import posidon.uranium.engine.nodes.spatial.voxel.Chunk
import posidon.uranium.main.Globals
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class Camera(
    name: String
) : Spatial(name) {

    var rotation = Vec2f(0f, 0f)
    var moveSpeed = 0.4f
    var jumpHeight = 0.4f
    var sensitivity = 0.4f

    var viewMatrix: Matrix4f = Matrix4f.view(position, rotation)

    private var velocity = Vec2f(0f, 0f)
    private val oldVelocity = Vec2f(0f, 0f)

    override fun update(delta: Double) {

        val movX by lazy { sin(Math.toRadians(rotation.y.toDouble())).toFloat() }
        val movZ by lazy { cos(Math.toRadians(rotation.y.toDouble())).toFloat() }

        val keys = booleanArrayOf(
            Input.isKeyDown(GLFW.GLFW_KEY_W),
            Input.isKeyDown(GLFW.GLFW_KEY_S),
            Input.isKeyDown(GLFW.GLFW_KEY_A),
            Input.isKeyDown(GLFW.GLFW_KEY_D))

        oldVelocity.set(velocity / 2f)
        velocity.set(0f, 0f)

        if (!(keys[0] && keys[1])) {
            if (keys[0]) {
                velocity.x -= movX
                velocity.y -= movZ
            } else if (keys[1]) {
                velocity.x += movX
                velocity.y += movZ
            }
        }
        if (!(keys[2] && keys[3])) {
            if (keys[2]) {
                velocity.x -= movZ
                velocity.y += movX
            } else if (keys[3]) {
                velocity.x += movZ
                velocity.y -= movX
            }
        }
        velocity = velocity.normalize() * delta.toFloat() * moveSpeed
        velocity += oldVelocity * max(1 - delta, 0.0).toFloat()

        position.x += velocity.x
        position.z += velocity.y

        if (Input.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            position.y += (jumpHeight * delta).toFloat()
        }
        if (Input.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            position.y -= (jumpHeight * delta).toFloat()
        }
        if (velocity.length != 0f) {
            Client.send(MovPacket(position))
        }
        viewMatrix = Matrix4f.view(position, rotation)
    }

    fun isPositionInFov(position: Vec3i): Boolean {
        val posRelToCam: Vec3i = position - this.position.toVec3i()
        val rotY = Math.toRadians((rotation.y - 180).toDouble())
        val cosRY = cos(rotY)
        val sinRY = sin(rotY)
        val rotX = Math.toRadians(rotation.x.toDouble())
        val cosRX = cos(rotX)
        val sinRX = sin(rotX)
        val x = (posRelToCam.x * cosRY - posRelToCam.z * sinRY) * cosRX + posRelToCam.y * sinRX
        val z = (posRelToCam.z * cosRY + posRelToCam.x * sinRY) * cosRX + posRelToCam.y * sinRX
        val y = posRelToCam.y * cosRX - z * sinRX
        val maxXOffset: Double = z * Window.width / Window.height + Chunk.SIZE * 1.5
        val maxYOffset = z * cosRX + posRelToCam.y * sinRX + Chunk.SIZE * 1.5
        return z > -Chunk.SIZE * 2 && x < maxXOffset && x > -maxXOffset && y < maxYOffset && y > -maxYOffset
    }

    override fun onEvent(event: Event) {
        when (event) {
            is MouseMovedEvent -> {
                rotation += Vec2f(
                        -sensitivity * event.cursorMovement.y,
                        -sensitivity * event.cursorMovement.x)
                if (rotation.x > 90) rotation.x = 90f
                else if (rotation.x < -90) rotation.x = -90f
                if (rotation.y > 360) rotation.y -= 360f
                else if (rotation.y < 0) rotation.y += 360f
            }
            is PacketReceivedEvent -> {
                when (event.tokens[0]) {
                    "pos" -> {
                        val coords = event.tokens[1].split(',')
                        position.set(coords[0].toFloat(), coords[1].toFloat(), coords[2].toFloat())
                    }
                    "rot" -> {
                        val coords = event.tokens[1].split(',')
                        rotation.set(coords[0].toFloat(), coords[1].toFloat())
                    }
                    "playerInfo" -> {
                        for (token in event.tokens) {
                            when {
                                token.startsWith("coords") -> {
                                    val coords = token.substring(7).split(',')
                                    position.x = coords[0].toFloat()
                                    position.y = coords[1].toFloat()
                                    position.z = coords[2].toFloat()
                                }
                                token.startsWith("movSpeed") -> moveSpeed = token.substring(10).toFloat()
                                token.startsWith("jmpHeight") -> jumpHeight = token.substring(11).toFloat()
                            }
                        }
                    }
                    "" -> Globals.running = false
                }
            }
        }
    }
}