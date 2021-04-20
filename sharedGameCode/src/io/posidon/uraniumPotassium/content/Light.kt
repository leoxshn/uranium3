package io.posidon.uraniumPotassium.content

import kotlin.math.max

data class Light(val i: Int, val light: Short) {
    companion object {
        const val R_MASK = 0b0000_0000_0000_1111
        const val G_MASK = 0b0000_0000_1111_0000
        const val B_MASK = 0b0000_1111_0000_0000
        const val I_MASK = 0b1111_0000_0000_0000
        const val C_MASK = 0b0000_1111_1111_1111

        inline fun blendLights(x: Int, y: Int): Int {
            val r = max(x and R_MASK, y and R_MASK)
            val g = max(x and G_MASK, y and G_MASK)
            val b = max(x and B_MASK, y and B_MASK)
            val i = max(x and I_MASK, y and I_MASK)
            return r or g or b or i
        }

        inline fun lightI(light: Int): Int = light and I_MASK
        inline fun lightColor(light: Int): Int = light and C_MASK

        inline fun make(r: Int, g: Int, b: Int, i: Int): Short {
            return (r or (g shl 4) or (b shl 8) or (i shl 12)).toShort()
        }
/*
        private const val COMPONENT_MASK = 0xf0f0f0f
        private const val BORROW_GUARD = 0x20202020
        private const val CARRY_MASK = 0x10101010

        private inline fun wlpHalfLT(a: Int, b: Int): Int {
            val d = (((a and COMPONENT_MASK) or BORROW_GUARD) - (b and COMPONENT_MASK)) and CARRY_MASK
            return (d ushr 1) or (d ushr 2) or (d ushr 3) or (d ushr 4)
        }

        fun wlpLT(a: Int, b: Int): Int
            = wlpHalfLT(a, b) or (wlpHalfLT(a shr 4, b shr 4) shl 4)

        inline fun wlpMax(a: Int, b: Int): Int = a xor (a xor b and wlpLT(a, b))

        private inline fun wlpDecHalf(x: Int): Int {
            val d = ((x and 0xf0f0f0f) or 0x20202020) - 0x1010101
            val b = d and 0x10101010
            return (d + (b shr 4)) and 0x0f0f0f0f
        }*/

        inline fun dec(light: Int): Int {
            val i = (lightI(light) shr 12) - 1
            if (i <= 0) {
                return 0
            }
            return (i shl 12) or lightColor(light)
        }
    }
}