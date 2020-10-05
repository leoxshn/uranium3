package posidon.uraniumGame

import posidon.library.types.Matrix4f
import posidon.library.types.Vec2f
import posidon.uraniumGame.events.PacketReceivedEvent
import posidon.uranium.input.Input
import posidon.uranium.input.events.Event
import posidon.uranium.input.events.MouseMovedEvent
import posidon.uranium.nodes.spatial.Camera
import posidon.uraniumGame.net.Client
import posidon.uraniumGame.net.packets.MovPacket
import posidon.uranium.input.Key
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class Player(
    name: String
) : Camera(name) {

    var moveSpeed = 0.4f
    var jumpHeight = 0.4f
    var sensitivity = 0.4f

    private var velocity = Vec2f(0f, 0f)
    private val oldVelocity = Vec2f(0f, 0f)

    override fun update(delta: Double) {

        val movX by lazy { sin(Math.toRadians(rotation.y.toDouble())).toFloat() }
        val movZ by lazy { cos(Math.toRadians(rotation.y.toDouble())).toFloat() }

        val keys = booleanArrayOf(
            Input.isKeyDown(Key.W),
            Input.isKeyDown(Key.S),
            Input.isKeyDown(Key.A),
            Input.isKeyDown(Key.D))

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

        if (Input.isKeyDown(Key.SPACE)) {
            position.y += (jumpHeight * delta).toFloat()
        }
        if (Input.isKeyDown(Key.LEFT_SHIFT)) {
            position.y -= (jumpHeight * delta).toFloat()
        }
        if (velocity.length != 0f) {
            Client.send(MovPacket(position))
        }
        viewMatrix = Matrix4f.view(position, rotation)
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
                }
            }
        }
    }
}