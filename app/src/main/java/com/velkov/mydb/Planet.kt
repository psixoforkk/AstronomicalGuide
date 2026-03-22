package com.velkov.mydb

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Planet(
    val name: String,
    val radius: Float,
    val distanceFromSun: Float,
    val speed: Float,
    val context: Context,
    val textureResId: Int,
    val hasMoon: Boolean = false,
    val isMoon: Boolean = false

) {
    private val texturedSphere: TexturedSphere
    private var moon: Planet? = null
    private var moonAngle = 0f
    private var orbitAngle = 0f

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val orbitPoints = 100
    private val orbitVertexBuffer: FloatBuffer
    private val orbitColor = floatArrayOf(1.0f, 1.0f, 1.0f, 0.3f)

    init {
        texturedSphere = TexturedSphere(context, radius, 30, 30, textureResId)

        if (!isMoon) {
            val orbitVertices = FloatArray(orbitPoints * 3)
            for (i in 0 until orbitPoints) {
                val angle = 2.0 * Math.PI * i / orbitPoints
                orbitVertices[i * 3] = (distanceFromSun * Math.cos(angle)).toFloat()
                orbitVertices[i * 3 + 1] = 0f
                orbitVertices[i * 3 + 2] = (distanceFromSun * Math.sin(angle)).toFloat()
            }

            orbitVertexBuffer = ByteBuffer.allocateDirect(orbitVertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(orbitVertices)
                    position(0)
                }
        } else {
            orbitVertexBuffer = ByteBuffer.allocateDirect(0).asFloatBuffer()
        }
    }

    fun update(deltaTime: Float) {
        orbitAngle += speed * deltaTime
        if (orbitAngle > 2 * Math.PI) {
            orbitAngle -= (2 * Math.PI).toFloat()
        }

        if (moon != null) {
            moonAngle += speed * 4f * deltaTime
        }
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {

        if (!isMoon) {
            drawOrbit(viewMatrix, projectionMatrix)
        }

        val x = distanceFromSun * cos(orbitAngle)
        val z = distanceFromSun * sin(orbitAngle)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0f, z)
        Matrix.rotateM(modelMatrix, 0, orbitAngle * 20, 0f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        texturedSphere.draw(mvpMatrix)

        moon?.let {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, 0f, z)

            val moonX = distanceFromSun * 0.15f * cos(moonAngle)
            val moonY = distanceFromSun * 0.15f * sin(moonAngle)

            Matrix.translateM(modelMatrix, 0, moonX, moonY, 0f)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            it.texturedSphere.draw(mvpMatrix)
        }
    }

    fun drawAsMoon(viewMatrix: FloatArray, projectionMatrix: FloatArray, parentEarth: Planet?) {
        parentEarth?.let { earth ->
            val earthX = earth.distanceFromSun * cos(earth.orbitAngle)
            val earthZ = earth.distanceFromSun * sin(earth.orbitAngle)

            val moonX = earthX + 1.2f * cos(orbitAngle)
            val moonZ = earthZ + 1.2f * sin(orbitAngle)

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, moonX, 0f, moonZ)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            texturedSphere.draw(mvpMatrix)
        }
    }

    private fun drawOrbit(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val orbitMVPMatrix = FloatArray(16)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(orbitMVPMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(orbitMVPMatrix, 0, projectionMatrix, 0, orbitMVPMatrix, 0)

        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """.trimIndent()

        val vertexShader = OpenGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = OpenGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        val program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        GLES20.glUseProgram(program)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val colorHandle = GLES20.glGetUniformLocation(program, "uColor")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, orbitMVPMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, orbitColor, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, orbitVertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, orbitPoints)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDeleteProgram(program)
    }

    private fun cos(angle: Float): Float = kotlin.math.cos(angle.toDouble()).toFloat()
    private fun sin(angle: Float): Float = kotlin.math.sin(angle.toDouble()).toFloat()
    fun getOrbitAngle(): Float = orbitAngle

    fun getPosition(): FloatArray {
        val x = distanceFromSun * cos(orbitAngle)
        val z = distanceFromSun * sin(orbitAngle)
        return floatArrayOf(x, 0f, z)
    }

}
