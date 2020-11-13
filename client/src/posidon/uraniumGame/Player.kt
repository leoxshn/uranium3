package posidon.uraniumGame

import posidon.library.types.Vec2f
import posidon.library.types.Vec3f
import posidon.uranium.graphics.Window
import posidon.uranium.input.Button
import posidon.uranium.events.PacketReceivedEvent
import posidon.uranium.input.Input
import posidon.uranium.events.Event
import posidon.uranium.events.MouseMovedEvent
import posidon.uranium.nodes.spatial.Eye
import posidon.uranium.net.Client
import posidon.uraniumGame.net.packets.MovPacket
import posidon.uranium.input.Key
import posidon.uranium.events.KeyPressedEvent
import posidon.uranium.events.MouseButtonPressedEvent
import posidon.uranium.nodes.Scene
import posidon.uranium.nodes.spatial.BoundingBox
import posidon.uranium.nodes.spatial.Spatial
import kotlin.math.*

class Player(
    name: String,
    val world: World
) : Spatial(name) {

    val normalFov = 72f
    val sprintFov = 95f
    val zoomFov = 24f

    var flySpeed = 24f

    var moveSpeed = 8f
    var jumpForce = 24f
    var sensitivity = 0.35f
    var gravity = false

    var sprintMultiplier = 2.4f


    val eye = Eye("eye").apply {
        position.y = .8f
        fov = normalFov
    }

    val boundingBox = BoundingBox("hitpox").apply {
        size.set(.6f, 2.4f, .6f)
    }

    init {
        add(eye)
        add(boundingBox)
    }

    private val velocity = Vec3f.zero()
    private val oldVelocity = Vec3f.zero()
    private var targetFov = normalFov

    override fun update(delta: Double) {

        val movX by lazy { sin(Math.toRadians(eye.rotation.y.toDouble())).toFloat() }
        val movZ by lazy { cos(Math.toRadians(eye.rotation.y.toDouble())).toFloat() }

        val isW = Input.isKeyDown(Key.W)
        val isA = Input.isKeyDown(Key.A)
        val isS = Input.isKeyDown(Key.S)
        val isD = Input.isKeyDown(Key.D)
        val isCtrl = Input.isKeyDown(Key.LEFT_CTRL)

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

        if (isCtrl && velocity.isNotZero()) {
            velocity.selfMultiply(sprintMultiplier)
            if (targetFov == normalFov) targetFov = sprintFov
        } else {
            if (targetFov == sprintFov) targetFov = normalFov
        }

        if (gravity) {
            velocity.selfMultiply(moveSpeed)
            if (!isOnSurface(boundingBox)) {
                velocity.y -= world.gravity
                velocity.selfBlend(oldVelocity, min(delta.toFloat().pow(1.35f) * FRICTION / 2f, 1f))
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
        } else {
            velocity.selfMultiply(flySpeed)
            velocity.selfBlend(oldVelocity, min(delta.toFloat() * FRICTION, 1f))
        }

        oldVelocity.set(velocity)
        velocity.selfMultiply(delta.toFloat())

        if (gravity) {
            moveAndSlide(boundingBox, velocity)
        } else {
            position.selfAdd(velocity)
        }

        if (velocity.length != 0f) {
            Client.send(MovPacket(position))
            eye.updateMatrix()
        }

        val transitionSpeed = delta.toFloat() * 15f
        eye.fov = (2f.pow(transitionSpeed) - 1f) * targetFov / 2f.pow(transitionSpeed) + eye.fov / 2f.pow(transitionSpeed)
    }

    private var timeOfLastSpacePress = 0L

    override fun onEvent(event: Event) {
        super.onEvent(event)
        when (event) {
            is MouseMovedEvent -> {
                eye.rotation += Vec2f(
                    -sensitivity * event.cursorMovement.y,
                    -sensitivity * event.cursorMovement.x)
                if (eye.rotation.x > 90) eye.rotation.x = 90f
                else if (eye.rotation.x < -90) eye.rotation.x = -90f
                if (eye.rotation.y > 360) eye.rotation.y -= 360f
                else if (eye.rotation.y < 0) eye.rotation.y += 360f
                eye.updateMatrix()
            }
            is PacketReceivedEvent -> {
                when (event.tokens[0]) {
                    "pos" -> {
                        val coords = event.tokens[1].split(',')
                        position.set(coords[0].toFloat(), coords[1].toFloat(), coords[2].toFloat())
                    }
                    "rot" -> {
                        val coords = event.tokens[1].split(',')
                        eye.rotation.set(coords[0].toFloat(), coords[1].toFloat())
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
                    Key.F11 -> if (event.action == Input.PRESS) Window.isFullscreen = !Window.isFullscreen
                    Key.ESCAPE -> if (event.action == Input.PRESS) Window.mouseLocked = false
                    Key.C -> when (event.action) {
                        Input.PRESS -> targetFov = zoomFov
                        Input.RELEASE -> targetFov = normalFov
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

    companion object {
        const val FRICTION = 30f
    }
}