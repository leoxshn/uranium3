package posidon.uranium.graphics

import org.lwjgl.opengl.*
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import kotlin.math.min

class Texture(filename: String) {

    private val id: Int = loadTexture(filename)

    fun destroy() = GL11.glDeleteTextures(id)

    var width = 0
    var height = 0

    private fun loadTexture(path: String): Int {
        var buf: ByteBuffer?
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            buf = STBImage.stbi_load(path, w, h, channels, 4)
            if (buf == null) throw Exception("Texture not loaded: [" + path + "] " + stbi_failure_reason())
            width = w.get()
            height = h.get()
        }
        val textureId = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, -1f)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE)
        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            val amount = min(4f, GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT))
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount)
        } else {
            println("error: Anisotropic filtering isn't supported")
        }
        STBImage.stbi_image_free(buf!!)
        return textureId
    }

    override fun toString() = "texture { id: $id, w: $width, h: $height }"

    companion object {
        fun bind(vararg textures: Texture?) {
            for (i in textures.indices) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i]?.id ?: 0)
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0)
        }
    }
}