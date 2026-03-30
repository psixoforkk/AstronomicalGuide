package com.velkov.mydb

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PostProcessShader {

    private val program: Int
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    private val positionHandle: Int
    private val texCoordHandle: Int
    private val sceneTextureHandle: Int
    private val timeHandle: Int
    private val resolutionHandle: Int
    private val blackHolePosHandle: Int

    private val vertices = floatArrayOf(
        -1f, -1f, 0f,
        1f, -1f, 0f,
        -1f,  1f, 0f,
        1f,  1f, 0f
    )

    private val texCoords = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); position(0) }

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(texCoords); position(0) }

        val vertexShaderCode = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision highp float;
            varying vec2 vTexCoord;
            uniform sampler2D uSceneTexture;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform vec2 uBlackHolePos;
            
            void main() {
                vec2 uv = vTexCoord;
                
                vec2 center = uBlackHolePos;
                vec2 tc = uv - center;
                float d = distance(uv, center);
                
                // Радиус чёрной дыры пульсирует
                float r = abs(sin(uTime / 10.0)) * 0.5;
                
                vec2 finalUv = uv;
                
                // Если внутри радиуса - искажаем
                if (d < r) {
                    float percent = clamp((r - d) / r, 0.0, 1.0);
                    float T = sin(uTime / 10.0);
                    float theta = percent * percent * T * 12.0;
                    
                    float s = sin(theta);
                    float c = cos(theta);
                    
                    //искажение
                    tc = vec2(dot(tc, vec2(c, -s)), dot(tc, vec2(s, c)));
                    finalUv = tc + center;
                }
                
                // Получаем цвет из отрендеренной сцены
                vec4 color = texture2D(uSceneTexture, finalUv);
                
                // Затемнение внутри чёрной дыры
                if (d < r) {
                    float percent = clamp((r - d) / r, 0.0, 1.0);
                    color.rgb *= pow(1.0 - percent, 2.5);
                }
                
                if (d < r * 0.25) {
                    color.rgb = vec3(0.0);
                }
                
                if (d > r * 0.7 && d < r) {
                    float glow = (d - r * 0.7) / (r * 0.3);
                    color.rgb += vec3(0.6, 0.2, 0.1) * (1.0 - glow) * 0.6;
                }
                
                gl_FragColor = color;
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        sceneTextureHandle = GLES20.glGetUniformLocation(program, "uSceneTexture")
        timeHandle = GLES20.glGetUniformLocation(program, "uTime")
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution")
        blackHolePosHandle = GLES20.glGetUniformLocation(program, "uBlackHolePos")
    }

    fun draw(sceneTextureId: Int, time: Float, width: Float, height: Float, bhX: Float, bhY: Float) {
        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glUniform1f(timeHandle, time)
        GLES20.glUniform2f(resolutionHandle, width, height)
        GLES20.glUniform2f(blackHolePosHandle, bhX, bhY)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sceneTextureId)
        GLES20.glUniform1i(sceneTextureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }
}