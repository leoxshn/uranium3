package posidon.potassium.content

class Block(
    val material: Material,
    val shape: Shape = Shape.CUBE
) {

    enum class Shape {
        CUBE,
        SLAB_BOTTOM,
        SLAB_TOP,
        SLAB_EAST,
        SLAB_WEST,
        SLAB_NORTH,
        SLAB_SOUTH,
        STAIRS_BOTTOM_EAST,
        STAIRS_BOTTOM_WEST,
        STAIRS_BOTTOM_NORTH,
        STAIRS_BOTTOM_SOUTH,
        STAIRS_TOP_EAST,
        STAIRS_TOP_WEST,
        STAIRS_TOP_NORTH,
        STAIRS_TOP_SOUTH,
        STAIRS_NORTH_EAST,
        STAIRS_NORTH_WEST,
        STAIRS_SOUTH_EAST,
        STAIRS_SOUTH_WEST
    }
}