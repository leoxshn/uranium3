package io.posidon.uranium.nodes.spatial

import io.posidon.library.types.Mat4f
import io.posidon.library.types.Vec2f
import io.posidon.library.types.Vec3f
import io.posidon.uranium.events.Event
import io.posidon.uranium.events.WindowResizedEvent
import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.graphics.Renderer.Companion.FAR
import io.posidon.uranium.graphics.Renderer.Companion.NEAR
import io.posidon.uranium.util.ProjectionMatrix
import kotlin.math.cos
import kotlin.math.sin

open class Eye(val renderer: Renderer) : Spatial() {

    var fov: Float = 70f
        set(value) {
            field = value
            if (renderer.eye === this) {
                projectionMatrix.setFovAndAspectRatio(value, renderer.window.width.toFloat() / renderer.window.height.toFloat())
            }
        }

    var rotation = Vec2f(0f, 0f)

    var projectionMatrix = ProjectionMatrix(0f, 0f, NEAR, FAR)
        private set

    var viewMatrix: Mat4f = Mat4f.view(globalTransform.position, rotation)
        private set

    var rotationMatrix: Mat4f = run {
        val rotX = Mat4f.rotateX(Math.toRadians(rotation.x.toDouble()))
        val rotY = Mat4f.rotateY(Math.toRadians(rotation.y.toDouble()))
        rotY * rotX
    }; private set

    override fun onEvent(event: Event) {
        if (renderer.eye === this && event is WindowResizedEvent) {
            projectionMatrix.setFovAndAspectRatio(fov, event.newWidth.toFloat() / event.newHeight.toFloat())
        }
    }

    fun updateMatrix() {
        viewMatrix = Mat4f.view(globalTransform.position, rotation)
        rotationMatrix = run {
            val rotX = Mat4f.rotateX(Math.toRadians(rotation.x.toDouble()))
            val rotY = Mat4f.rotateY(Math.toRadians(rotation.y.toDouble()))
            rotY * rotX
        }
    }

    fun getDirection(): Vec3f {

        val radx = Math.toRadians(rotation.x.toDouble())
        val rady = Math.toRadians(rotation.y.toDouble())

        //Rotate X
        val y = sin(radx)
        var z = -cos(radx)

        //Rotate Y
        val x = z * sin(rady)
        z *= cos(rady)

        return Vec3f(x.toFloat(), y.toFloat(), z.toFloat())
    }
}