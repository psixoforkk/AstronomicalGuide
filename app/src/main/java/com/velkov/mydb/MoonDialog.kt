package com.velkov.mydb

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun MoonDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss,properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {

            AndroidView(
                factory = { ctx ->
                    MoonGLView(ctx)
                },
                modifier = Modifier.fillMaxSize()
            )
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Text("Закрыть")
            }

            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                Text(
                    text = "Луна | Радиус: 1737 км | Расстояние до земли: 384400 км",
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

class MoonGLView(context: Context) : GLSurfaceView(context) {
    private val renderer: MoonRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MoonRenderer()
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}

class MoonRenderer : GLSurfaceView.Renderer {

    private var mVertexBuffer: FloatBuffer? = null
    private var mNormalBuffer: FloatBuffer? = null
    private var n = 0

    private var xCamera = 0.5f
    private var yCamera = 1.5f
    private var zCamera = 1.2f

    private var xL = 1.5f
    private var yL = 2f
    private var zL = 1.5f

    private var W = 0f
    private var H = 0f

    private var mProgram = 0

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    private val vertexShaderCode = """
        uniform mat4 u_MVPMatrix;
        attribute vec3 a_vertex;
        attribute vec3 a_normal;
        varying vec3 v_vertex;
        varying vec3 v_normal;
        void main() {
            v_vertex = a_vertex;
            v_normal = normalize(a_normal);
            gl_Position = u_MVPMatrix * vec4(a_vertex, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec3 u_camera;
        uniform vec3 u_lightPosition;
        varying vec3 v_vertex;
        varying vec3 v_normal;
        void main() {
            vec3 n_normal = normalize(v_normal);
            vec3 lightvector = normalize(u_lightPosition - v_vertex);
            vec3 lookvector = normalize(u_camera - v_vertex);
            float ambient = 0.25;
            float k_diffuse = 0.7;
            float k_specular = 0.5;
            float diffuse = k_diffuse * max(dot(n_normal, lightvector), 0.0);
            vec3 reflectvector = reflect(-lightvector, n_normal);
            float specular = k_specular * pow(max(dot(lookvector, reflectvector), 0.0), 32.0);
            vec3 moonColor = vec3(0.85, 0.8, 0.75);
            vec3 finalColor = (ambient + diffuse + specular) * moonColor;
            gl_FragColor = vec4(finalColor, 1.0);
        }
    """.trimIndent()

    init {
        initNIO(1.2f)
    }

    private fun initNIO(R: Float) {
        val dtheta = 12
        val dphi = 12
        val DTOR = (Math.PI / 180.0).toFloat()

        val verticesList = mutableListOf<Float>()
        val normalsList = mutableListOf<Float>()
        n = 0

        for (theta in -90..(90 - dtheta) step dtheta) {
            for (phi in 0..(360 - dphi) step dphi) {
                val x1 = Math.cos(theta * DTOR.toDouble()).toFloat() *
                        Math.cos(phi * DTOR.toDouble()).toFloat() * R
                val y1 = Math.cos(theta * DTOR.toDouble()).toFloat() *
                        Math.sin(phi * DTOR.toDouble()).toFloat() * R
                val z1 = Math.sin(theta * DTOR.toDouble()).toFloat() * R

                val x2 = Math.cos((theta + dtheta) * DTOR.toDouble()).toFloat() *
                        Math.cos(phi * DTOR.toDouble()).toFloat() * R
                val y2 = Math.cos((theta + dtheta) * DTOR.toDouble()).toFloat() *
                        Math.sin(phi * DTOR.toDouble()).toFloat() * R
                val z2 = Math.sin((theta + dtheta) * DTOR.toDouble()).toFloat() * R

                val x3 = Math.cos((theta + dtheta) * DTOR.toDouble()).toFloat() *
                        Math.cos((phi + dphi) * DTOR.toDouble()).toFloat() * R
                val y3 = Math.cos((theta + dtheta) * DTOR.toDouble()).toFloat() *
                        Math.sin((phi + dphi) * DTOR.toDouble()).toFloat() * R
                val z3 = Math.sin((theta + dtheta) * DTOR.toDouble()).toFloat() * R

                val x4 = Math.cos(theta * DTOR.toDouble()).toFloat() *
                        Math.cos((phi + dphi) * DTOR.toDouble()).toFloat() * R
                val y4 = Math.cos(theta * DTOR.toDouble()).toFloat() *
                        Math.sin((phi + dphi) * DTOR.toDouble()).toFloat() * R
                val z4 = Math.sin(theta * DTOR.toDouble()).toFloat() * R

                verticesList.addAll(listOf(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4))

                val len1 = Math.sqrt((x1 * x1 + y1 * y1 + z1 * z1).toDouble()).toFloat()
                val len2 = Math.sqrt((x2 * x2 + y2 * y2 + z2 * z2).toDouble()).toFloat()
                val len3 = Math.sqrt((x3 * x3 + y3 * y3 + z3 * z3).toDouble()).toFloat()
                val len4 = Math.sqrt((x4 * x4 + y4 * y4 + z4 * z4).toDouble()).toFloat()

                normalsList.addAll(listOf(
                    x1 / len1, y1 / len1, z1 / len1,
                    x2 / len2, y2 / len2, z2 / len2,
                    x3 / len3, y3 / len3, z3 / len3,
                    x4 / len4, y4 / len4, z4 / len4
                ))

                n += 4
            }
        }

        mVertexBuffer = ByteBuffer.allocateDirect(verticesList.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(verticesList.toFloatArray())
                position(0)
            }

        mNormalBuffer = ByteBuffer.allocateDirect(normalsList.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(normalsList.toFloatArray())
                position(0)
            }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun shader1() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.02f, 0.03f, 1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        W = width.toFloat()
        H = height.toFloat()
        shader1()
    }

    private fun calcMatrix() {
        val ratio = W / H
        val k = 0.2f
        val left = -k * ratio
        val right = k * ratio
        val bottom = -k
        val top = k
        val near = 0.1f
        val far = 10.0f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, 0f)

        Matrix.setLookAtM(viewMatrix, 0, xCamera, yCamera, zCamera, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        calcMatrix()

        GLES20.glUseProgram(mProgram)

        val uCameraHandle = GLES20.glGetUniformLocation(mProgram, "u_camera")
        GLES20.glUniform3f(uCameraHandle, xCamera, yCamera, zCamera)

        val uLightHandle = GLES20.glGetUniformLocation(mProgram, "u_lightPosition")
        GLES20.glUniform3f(uLightHandle, xL, yL, zL)

        val uMvpHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix")
        GLES20.glUniformMatrix4fv(uMvpHandle, 1, false, modelViewProjectionMatrix, 0)

        val aVertexHandle = GLES20.glGetAttribLocation(mProgram, "a_vertex")
        GLES20.glEnableVertexAttribArray(aVertexHandle)
        GLES20.glVertexAttribPointer(aVertexHandle, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer)

        val aNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_normal")
        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 0, mNormalBuffer)

        for (i in 0 until n step 4) {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, i, 4)
        }

        GLES20.glDisableVertexAttribArray(aVertexHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
    }
}