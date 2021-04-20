package io.posidon.uraniumGame

import io.posidon.library.types.Vec2f
import io.posidon.library.types.Vec3f
import io.posidon.library.types.Vec3i
import io.posidon.uranium.events.Event
import io.posidon.uranium.events.KeyPressedEvent
import io.posidon.uranium.events.MouseButtonPressedEvent
import io.posidon.uranium.events.MouseMovedEvent
import io.posidon.uranium.graphics.Filter
import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.input.Button
import io.posidon.uranium.input.Input
import io.posidon.uranium.input.Key
import io.posidon.uranium.net.client.ClientApi
import io.posidon.uranium.nodes.spatial.BoundingBox
import io.posidon.uranium.nodes.spatial.Eye
import io.posidon.uranium.nodes.spatial.Spatial
import io.posidon.uraniumGame.voxel.ChunkMap
import io.posidon.uraniumPotassium.content.worldGen.Constants
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class Player(
    renderer: Renderer,
    val world: World,
    val speedEffect: Filter
) : Spatial() {

    val normalFov = 72f
    val sprintFov = 95f
    val zoomFov = 24f

    var flySpeed = 18f

    var initGravity = 36f
    var gravityAcceleration = 0.8f

    var moveSpeed = 7f
    var jumpForce = 16.5f
    var sensitivity = 0.35f
    var doGravity = false

    var sprintMultiplier = 1.5f

    val eye = Eye(renderer).apply {
        position.y = .8f
        fov = normalFov
    }

    val boundingBox = BoundingBox().apply {
        size.set(.6f, 2.4f, .6f)
    }

    init {
        add(eye)
        add(boundingBox)
    }

    private val velocity = Vec3f.zero()
    private val oldVelocity = Vec3f.zero()
    private var targetFov = normalFov
    private var fallSpeed = initGravity

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

        if (!doGravity) {
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
            speedEffect.enabled = true
        } else {
            if (targetFov == sprintFov) targetFov = normalFov
            speedEffect.enabled = false
        }

        if (doGravity) {
            velocity.selfMultiply(moveSpeed)
            if (!isOnSurface(boundingBox, maxDistanceFromSurface = 0.2f)) {
                velocity.y -= fallSpeed
                fallSpeed *= 1 + gravityAcceleration * delta.toFloat()
                velocity.selfBlend(oldVelocity, min(delta.toFloat().pow(1.35f) * FRICTION / 2f, 1f))
            } else {
                fallSpeed = initGravity
                velocity.selfBlend(oldVelocity, min(delta.toFloat() * FRICTION, 1f))
                if (Input.isKeyDown(Key.SPACE)) {
                    velocity.y = jumpForce
                    oldVelocity.y = jumpForce * 2f
                } else {
                    velocity.y = 0f
                    oldVelocity.y = 0f
                }
            }
        } else {
            fallSpeed = initGravity
            velocity.selfMultiply(flySpeed)
            velocity.selfBlend(oldVelocity, min(delta.toFloat() * FRICTION, 1f))
        }

        oldVelocity.set(velocity)
        velocity.selfMultiply(delta.toFloat())

        if (doGravity) {
            moveAndSlide(boundingBox, velocity)
        } else {
            position.selfAdd(velocity)
        }

        if (velocity.length != 0f) {
            val worldSize = ChunkMap.sizeInVoxels
            if (position.x < 0) {
                position.x = worldSize + position.x % worldSize
            } else if (position.x >= worldSize) {
                position.x = position.x % worldSize
            }
            if (position.z < 0) {
                position.z = worldSize + position.z % worldSize
            } else if (position.z >= worldSize) {
                position.z = position.z % worldSize
            }
            Client.send(ClientApi.goto(position))
            eye.updateMatrix()
            world.updateCoords()
            selectBlock(world.chunkMap, 7)
        }

        val transitionSpeed = delta.toFloat() * 15f
        eye.fov = (2f.pow(transitionSpeed) - 1f) * targetFov / 2f.pow(transitionSpeed) + eye.fov / 2f.pow(transitionSpeed)
    }

    private var timeOfLastSpacePress = 0L

    val currentBlockSelection = Vec3i(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

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
                selectBlock(world.chunkMap, 7)
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
                    Key.F11 -> if (event.action == Input.PRESS) event.window.isFullscreen = !event.window.isFullscreen
                    Key.ESCAPE -> if (event.action == Input.PRESS) event.window.mouseLocked = false
                    Key.C -> when (event.action) {
                        Input.PRESS -> targetFov = zoomFov
                        Input.RELEASE -> targetFov = normalFov
                    }
                    Key.SPACE -> {
                        if (event.action == Input.PRESS) {
                            val timeDifference = event.millis - timeOfLastSpacePress
                            if (timeDifference < 200) {
                                timeOfLastSpacePress = 0L
                                doGravity = !doGravity
                            } else {
                                timeOfLastSpacePress = event.millis
                            }
                        }
                    }
                }
            }
            is MouseButtonPressedEvent -> {
                if (event.button == Button.MOUSE_LEFT && event.action == Input.PRESS) {
                    if (!event.window.mouseLocked) event.window.mouseLocked = true
                    else if (currentBlockSelection.x != Int.MAX_VALUE) {
                        currentBlockSelection.let {
                            world.chunkMap.setBlock(it, null)?.let { world.chunkMap.generateChunkMesh(it) }
                            Client.send(ClientApi.breakBlock(it))
                            selectBlock(world.chunkMap, 7)
                        }
                    }
                }
            }
        }
    }

    private fun selectBlock(chunkMap: ChunkMap, maxDistance: Int) {
        val stepSize = .01f
        var i = 0
        val step = eye.getDirection() * stepSize
        val p = eye.globalTransform.position.copy()
        while (i * stepSize < maxDistance) {
            if (p.y < 0 || p.y >= ChunkMap.heightInChunks * Constants.CHUNK_SIZE) {
                return currentBlockSelection.set(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
            }
            val pos = p.floorToVec3i()
            pos.x = ChunkMap.clipVoxelHorizontal(pos.x)
            pos.z = ChunkMap.clipVoxelHorizontal(pos.z)
            chunkMap.getBlock(pos)?.let {
                return currentBlockSelection.set(pos)
            }
            p.selfAdd(step)
            i++
        }
        currentBlockSelection.set(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
    }

    companion object {
        const val FRICTION = 30f
    }
}