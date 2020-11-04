package posidon.uranium.nodes.spatial

import posidon.library.types.Matrix4f
import posidon.library.types.Vec2f
import posidon.library.types.Vec3i
import posidon.uranium.events.Event
import posidon.uranium.events.WindowResizedEvent
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Window
import kotlin.math.cos
import kotlin.math.sin

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

    override fun onEvent(event: Event) {
        if (Renderer.eye === this && event is WindowResizedEvent) {
            Renderer.eye?.fov?.let { Renderer.projectionMatrix.setFovAndAspectRatio(it, event.newWidth.toFloat() / event.newHeight.toFloat()) }
        }
    }
}