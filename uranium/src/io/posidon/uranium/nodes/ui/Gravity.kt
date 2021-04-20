package io.posidon.uranium.nodes.ui

object Gravity {

    const val TOP:                      Int = 0b1000
    const val BOTTOM:                   Int = 0b0100
    const val CENTER_VERTICAL:          Int = TOP or BOTTOM

    const val LEFT:                     Int = 0b0010
    const val RIGHT:                    Int = 0b0001
    const val CENTER_HORIZONTAL:        Int = LEFT or RIGHT

    const val CENTER:                   Int = CENTER_HORIZONTAL or CENTER_VERTICAL
}