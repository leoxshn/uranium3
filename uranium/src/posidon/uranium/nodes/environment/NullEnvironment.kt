package posidon.uranium.nodes.environment

import posidon.library.types.Vec3f

class NullEnvironment : Environment {

    override val skyColor: Vec3f get() = Vec3f.ZERO
    override val ambientLight: Vec3f get() = Vec3f.ZERO

    override var sun: Sun? = null

    override fun update(delta: Double) {}
}