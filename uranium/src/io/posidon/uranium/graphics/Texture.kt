package io.posidon.uranium.graphics

import io.posidon.uranium.graphics.opengl.OpenGLRenderer
import io.posidon.uranium.util.Stack
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_failure_reason
import java.nio.ByteBuffer

class Texture internal constructor(
    private var id: Int,
    var width: Int,
    var height: Int
) {

    fun destroy() = GL11.glDeleteTextures(id)

    override fun toString() = "texture { id: $id, w: $width, h: $height }"

    fun setWrap(wrap: Wrap) {
        bind(this)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap.value)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap.value)
    }

    fun setMagFilter(filter: MagFilter) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter.value)
    }
    
    fun setMinFilter(filter: MinFilter) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter.value)
    }

    private var tmpBuffer: ByteBuffer? = null

    fun create() {
        id = OpenGLRenderer.createTexture(tmpBuffer!!, width, height)
    }

    companion object {
        fun bind(vararg textures: Texture?) {
            for (i in textures.indices) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]?.id ?: 0)
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0)
        }

        fun load(path: String): Texture {
            var buf: ByteBuffer?
            val width: Int
            val height: Int
            Stack.push { stack ->
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)
                val channels = stack.mallocInt(1)
                buf = STBImage.stbi_load(path, w, h, channels, 4)
                if (buf == null) throw Exception("Texture not loaded: [" + path + "] " + stbi_failure_reason())
                width = w.get()
                height = h.get()
            }
            return Texture(0, width, height).also { it.tmpBuffer = buf }
        }
    }

    enum class Wrap(internal val value: Int) {
        REPEAT(GL11.GL_REPEAT),
        MIRRORED_REPEAT(GL14.GL_MIRRORED_REPEAT),
        CLAMP_TO_EDGE(GL14.GL_CLAMP_TO_EDGE),
        CLAMP_TO_BORDER(GL14.GL_CLAMP_TO_BORDER)
    }

    enum class MinFilter(internal val value: Int) {
        NEAREST(GL11.GL_NEAREST_MIPMAP_NEAREST),
        SMOOTHER_NEAREST(GL11.GL_LINEAR_MIPMAP_NEAREST),
        LINEAR(GL11.GL_LINEAR_MIPMAP_LINEAR)
    }

    enum class MagFilter(internal val value: Int) {
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR),
    }
}