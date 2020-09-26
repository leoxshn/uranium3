package posidon.uranium.content

import posidon.uranium.engine.nodes.Node
import posidon.uranium.engine.nodes.spatial.Camera

class World : Node("World") {

    val camera = Camera("camera")

    init {
        add(camera)
    }
}