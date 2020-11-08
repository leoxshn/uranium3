package posidon.uranium.nodes.spatial

import posidon.uranium.graphics.Matrix4f
import posidon.library.types.Vec2f
import posidon.uranium.events.Event
import posidon.uranium.events.WindowResizedEvent
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Window

open class Eye(
    name: String
) : Spatial(name) {

    var fov: Float = 70f
        set(value) {
            field = value
            if (Renderer.eye === this) {
                Renderer.projectionMatrix.setFovAndAspectRatio(value, Window.width.toFloat() / Window.height.toFloat())
            }
        }

    var rotation = Vec2f(0f, 0f)
    var viewMatrix: Matrix4f = Matrix4f.view(globalTransform.position, rotation)
        private set
    var rotationMatrix: Matrix4f = run {
        val rotX = Matrix4f.rotateX(Math.toRadians(rotation.x.toDouble()))
        val rotY = Matrix4f.rotateY(Math.toRadians(rotation.y.toDouble()))
        rotY * rotX
    }; private set

    override fun onEvent(event: Event) {
        if (Renderer.eye === this && event is WindowResizedEvent) {
            Renderer.eye?.fov?.let { Renderer.projectionMatrix.setFovAndAspectRatio(it, event.newWidth.toFloat() / event.newHeight.toFloat()) }
        }
    }

    fun updateMatrix() {
        viewMatrix = Matrix4f.view(globalTransform.position, rotation)
        rotationMatrix = run {
            val rotX = Matrix4f.rotateX(Math.toRadians(rotation.x.toDouble()))
            val rotY = Matrix4f.rotateY(Math.toRadians(rotation.y.toDouble()))
            rotY * rotX
        }
    }
}