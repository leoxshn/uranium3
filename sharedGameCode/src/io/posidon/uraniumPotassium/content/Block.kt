package io.posidon.uraniumPotassium.content

import io.posidon.library.types.Vec2i

enum class Block(
    val id: String,
    /**
     * Stores the RGB channels and an intensity channel, each occupying 4 bits
     * r[0..15] g[0..15] b[0..15] intensity[0..15]
     */
    val light: Short = 0,
    val isOpaque: Boolean = true,
    val uv: Vec2i = Vec2i(7, 7)
) {

    // Natural
    DIRT("dirt", uv = Vec2i(0, 0)),
    RED_SAND("red_sand"),
    ROSE_SAND("rose_sand"),
    WHITE_SAND("white_sand"),
    BLACK_SAND("black_sand"),
    STONE("stone", uv = Vec2i(1, 0)),
    CRUNCHY_STONE("crunchy_stone"),
    GRAVEL("gravel", uv = Vec2i(1, 2)),
    MOONSTONE("moonstone", light = Light.make(3, 5, 10, 3), uv = Vec2i(2, 0)),

    SHARP_STONE("sharp_stone", uv = Vec2i(2, 2)),
    COPPER_SULFATE("cu_sulfate"),

    SLIME("slime", uv = Vec2i(3, 0)),

    // Ores
    COPPER_ORE("cu_ore"),
    RUBY_ORE("ruby_ore"),
    GOLD_ORE("au_ore"),
    ALUMINUM_ORE("aluminum_ore"),
    EMERALD_ORE("emerald_ore"),
    SAPPHIRE_ORE("sapphire_ore"),
    MALACHITE_ORE("malachite_ore"),

    // Ore -> Block
    COPPER("cu"),
    RUBY("ruby"),
    GOLD("au"),
    ALUMINUM("aluminum"),
    EMERALD("emerald"),
    SAPPHIRE("sapphire"),
    MALACHITE("malachite"),


    // Artificial
    WOOD("wood", uv = Vec2i(0, 1)),
    LIGHT_BRICKS("light_bricks", light = Light.make(15, 14, 6, 15), uv = Vec2i(1, 1)),
    MOONSTONE_BRICKS("moonstone_bricks", light = Light.make(0, 6, 15, 6), uv = Vec2i(2, 1)),
    GLOW_CUBE("glow_cube", light = Light.make(15, 15, 15, 15), uv = Vec2i(3, 1)),
    GLASS("glass"),
    BLACK_GLASS("black_glass"),

    UNKNOWN("?");

    fun getSaveString(): String {
        return id
    }
}