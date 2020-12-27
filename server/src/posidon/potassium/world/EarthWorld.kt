package posidon.potassium.world

import posidon.potassium.world.gen.EarthWorldGenerator

class EarthWorld(seed: Long) : World(16, 8) {

    override val generator = EarthWorldGenerator(seed, sizeInVoxels, heightInVoxels)
    override val name = "terra"
}