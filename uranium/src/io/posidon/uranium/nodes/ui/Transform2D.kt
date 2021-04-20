package io.posidon.uranium.nodes.ui

import io.posidon.library.types.Vec2f
import io.posidon.library.types.Vec2i

class Transform2D (
    val translation: Vec2i = Vec2i.zero(),
    val size: Vec2i,
    val scale: Vec2f,
    var keepAspectRatio: Boolean
)