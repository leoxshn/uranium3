package posidon.potassium.world

import posidon.potassium.world.gen.EarthWorldGenerator

class EarthWorld(seed: Long) : World() {

    override val generator = EarthWorldGenerator(seed)
    override val name = "terra"
}