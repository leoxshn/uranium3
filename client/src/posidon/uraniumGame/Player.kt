package posidon.uraniumGame

import posidon.library.types.Matrix4f
import posidon.library.types.Vec2f
import posidon.library.types.Vec3f
import posidon.uranium.graphics.Window
import posidon.uranium.input.Button
import posidon.uraniumGame.events.PacketReceivedEvent
import posidon.uranium.input.Input
import posidon.uranium.events.Event
import posidon.uranium.events.MouseMovedEvent
import posidon.uranium.nodes.spatial.Camera
import posidon.uraniumGame.net.Client
import posidon.uraniumGame.net.packets.MovPacket
import posidon.uranium.input.Key
import posidon.uranium.events.KeyPressedEvent
import posidon.uranium.events.MouseButtonPressedEvent
import kotlin.math.*

class Player(
    name: String,
    val world: World
) : Camera(name) {

    companion object {
        const val FRICTION = 25f
    }

    var moveSpeed = 50f
    var jumpForce = 50f
    var sensitivity = 0.4f
    var gravity = false

    private val velocity = Vec3f.zero()
    private val oldVelocity = Vec3f.zero()

    override fun update(delta: Double) {

        val movX by lazy { sin(Math.toRadians(rotation.y.toDouble())).toFloat() }
        val movZ by lazy { cos(Math.toRadians(rotation.y.toDouble())).toFloat() }

        val isW = Input.isKeyDown(Key.W)
        val isA = Input.isKeyDown(Key.A)
        val isS = Input.isKeyDown(Key.S)
        val isD = Input.isKeyDown(Key.D)

        velocity.set(0f, 0f, 0f)

        if (!(isW && isS)) {
            if (isW) {
                velocity.x -= movX
                velocity.z -= movZ
            } else if (isS) {
                velocity.x += movX
                velocity.z += movZ
            }
        }
        if (!(isA && isD)) {
            if (isA) {
                velocity.x -= movZ
                velocity.z += movX
            } else if (isD) {
                velocity.x += movZ
                velocity.z -= movX
            }
        }

        if (!gravity) {
            if (Input.isKeyDown(Key.SPACE)) {
                velocity.y++
            }
            if (Input.isKeyDown(Key.LEFT_SHIFT)) {
                velocity.y--
            }
        }

        velocity.selfNormalize()
        velocity.selfMultiply(moveSpeed)

        if (gravity) {
            val legPos = position.copy(y = position.y - 2)
            if (world.getBlock(legPos) == null) {
                velocity.y -= world.gravity
                velocity.selfBlend(oldVelocity, min(delta.toFloat() * FRICTION, 1f))
            } else {
                velocity.selfBlend(oldVelocity, min(delta.toFloat() * FRICTION, 1f))
                if (Input.isKeyDown(Key.SPACE)) {
                    velocity.y = jumpForce
                    oldVelocity.y = jumpForce
                } else {
                    velocity.y = 0f
                    oldVelocity.y = 0f
                }
            }
        }

        oldVelocity.set(velocity)
        velocity.selfMultiply(delta.toFloat())

        position.selfAdd(velocity)

        if (velocity.length != 0f) {
            Client.send(MovPacket(position))
        }
        viewMatrix = Matrix4f.view(position, rotation)
    }

    private var timeOfLastSpacePress = 0L

    override fun onEvent(event: Event) {
        super.onEvent(event)
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
                                token.startsWith("jmpHeight") -> jumpForce = token.substring(11).toFloat()
                            }
                        }
                    }
                }
            }
            is KeyPressedEvent -> {
                when (event.key) {
                    Key.F11 -> Window.isFullscreen = !Window.isFullscreen
                    Key.ESCAPE -> Window.mouseLocked = false
                    Key.C -> when (event.action) {
                        Input.PRESS -> fov = 20f
                        Input.RELEASE -> fov = 70f
                    }
                    Key.SPACE -> {
                        if (event.action == Input.PRESS) {
                            val timeDifference = event.millis - timeOfLastSpacePress
                            if (timeDifference < 200) {
                                timeOfLastSpacePress = 0L
                                gravity = !gravity
                            } else {
                                timeOfLastSpacePress = event.millis
                            }
                        }
                    }
                }
            }
            is MouseButtonPressedEvent -> {
                if (event.button == Button.MOUSE_LEFT) Window.mouseLocked = true
            }
        }
    }
}