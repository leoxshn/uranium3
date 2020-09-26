package posidon.uranium.content

import posidon.uranium.engine.nodes.Environment
import posidon.uranium.engine.nodes.Node
import posidon.uranium.engine.nodes.spatial.Camera
import posidon.uranium.engine.nodes.spatial.voxel.ChunkMap
//import posidon.uranium.engine.nodes.ui.HotBarComponent

class World : Node("World") {

    val camera = Camera("camera")
    val chunkMap = ChunkMap("chunks")
    //val hotBar = HotBarComponent("hotBar")

    init {
        add(camera)
        add(chunkMap)
        //add(hotBar)
        add(Environment)
    }
}