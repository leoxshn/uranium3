package posidon.uraniumGame

import posidon.uranium.nodes.Environment
import posidon.uranium.nodes.Node
import posidon.uraniumGame.voxel.ChunkMap
import posidon.uranium.nodes.ui.HotBarComponent

class World : Node("World") {

    val camera = Player("camera")
    val chunkMap = ChunkMap("chunks")
    val hotBar = HotBarComponent("hotBar")

    init {
        add(camera)
        add(chunkMap)
        add(hotBar)
        add(Environment)
    }
}