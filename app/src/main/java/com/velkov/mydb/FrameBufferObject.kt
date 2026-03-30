package com.velkov.mydb

import android.opengl.GLES20
import java.nio.IntBuffer

class FrameBufferObject(width: Int, height: Int) {

    var frameBuffer = 0
    var renderedTexture = 0
    var depthRenderBuffer = 0

    private var fboWidth = width
    private var fboHeight = height

    init {
        setupFrameBuffer()
    }

    private fun setupFrameBuffer() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        renderedTexture = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderedTexture)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, fboWidth, fboHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val framebuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, framebuffers, 0)
        frameBuffer = framebuffers[0]

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer)

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderedTexture, 0)

        val renderbuffers = IntArray(1)
        GLES20.glGenRenderbuffers(1, renderbuffers, 0)
        depthRenderBuffer = renderbuffers[0]

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRenderBuffer)
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, fboWidth, fboHeight)
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRenderBuffer)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer)
        GLES20.glViewport(0, 0, fboWidth, fboHeight)
    }

    fun unbind() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun getTextureId(): Int = renderedTexture

    fun resize(width: Int, height: Int) {
        if (fboWidth != width || fboHeight != height) {
            fboWidth = width
            fboHeight = height
            cleanup()
            setupFrameBuffer()
        }
    }

    fun cleanup() {
        GLES20.glDeleteFramebuffers(1, IntBuffer.wrap(intArrayOf(frameBuffer)))
        GLES20.glDeleteTextures(1, IntBuffer.wrap(intArrayOf(renderedTexture)))
        GLES20.glDeleteRenderbuffers(1, IntBuffer.wrap(intArrayOf(depthRenderBuffer)))
    }
}