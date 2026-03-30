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
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun NeptuneDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AndroidView(
                factory = { ctx ->
                    NeptuneGLView(ctx)
                },
                modifier = Modifier.fillMaxSize()
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(" Закрыть")
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                Text(
                    text = "Нептун | Радиус: 24 622 км | Планета-океан | Поверхность с волнами",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

class NeptuneGLView(context: Context) : GLSurfaceView(context) {
    private val renderer: NeptuneRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = NeptuneRenderer()
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}

class NeptuneRenderer : GLSurfaceView.Renderer {

    private val radius = 1.2f
    private val innerRadius = 1.20f
    private val stacks = 60
    private val slices = 60

    private val N = 80
    private val K = 0.06f
    private val DT = 0.1f
    private lateinit var points: Array<Array<WaterPoint>>

    private var innerVertexBuffer: FloatBuffer? = null
    private var innerNormalBuffer: FloatBuffer? = null
    private var innerIndexBuffer: ShortBuffer? = null
    private var outerVertexBuffer: FloatBuffer? = null
    private var indexCount = 0

    private var xCamera = 0f
    private var yCamera = 0.5f
    private var zCamera = 1.2f

    private var xL = 1.5f
    private var yL = 2f
    private var zL = 2f

    private var W = 0f
    private var H = 0f
    private var mProgram = 0
    private var mProgramPoints = 0

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
            
            float ambient = 0.3;
            float k_diffuse = 0.7;
            float k_specular = 0.4;
            
            float diffuse = k_diffuse * max(dot(n_normal, lightvector), 0.0);
            
            vec3 reflectvector = reflect(-lightvector, n_normal);
            float specular = k_specular * pow(max(dot(lookvector, reflectvector), 0.0), 32.0);
            
            vec3 waterColor = vec3(0.2, 0.5, 0.9);
            vec3 finalColor = (ambient + diffuse + specular) * waterColor;
            
            gl_FragColor = vec4(finalColor, 1.0);
        }
    """.trimIndent()

    private val vertexShaderPointsCode = """
        uniform mat4 u_MVPMatrix;
        attribute vec3 a_vertex;
        void main() {
            gl_PointSize = 2.5;
            gl_Position = u_MVPMatrix * vec4(a_vertex, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderPointsCode = """
        precision mediump float;
        uniform vec3 uColor;
        void main() {
            gl_FragColor = vec4(uColor, 0.9);
        }
    """.trimIndent()

    data class WaterPoint(
        var x: Float = 0f,
        var y: Float = 0f,
        var z: Float = 0f,
        var vz: Float = 0f
    )

    private fun sqr(x: Float) = x * x

    private fun initWaterPhysics() {
        points = Array(N) { i ->
            Array(N) { j ->
                WaterPoint().apply {
                    x = j.toFloat() / N
                    y = i.toFloat() / N
                    z = 0f
                    vz = 0f
                }
            }
        }
    }

    private fun pushWave() {
        if (Math.random() * 500 > 10) return

        val x0 = (Math.random() * N / 2 + 1).toInt()
        val y0 = (Math.random() * N / 2 + 1).toInt()

        for (y in y0 - 5 until y0 + 5) {
            if (y < 1 || y >= N - 1) continue
            for (x in x0 - 5 until x0 + 5) {
                if (x < 1 || x >= N - 1) continue
                val dist = sqrt(sqr((y - y0).toFloat()) + sqr((x - x0).toFloat()))
                points[x][y].z = 10f / N - (dist * 1.0f / N)
            }
        }
    }

    private fun updateWaves() {
        val dx = intArrayOf(-1, 0, 1, 0)
        val dy = intArrayOf(0, 1, 0, -1)

        pushWave()

        for (y in 1 until N - 1) {
            for (x in 1 until N - 1) {
                val p0 = points[x][y]
                for (i in 0 until 4) {
                    val p1 = points[x + dx[i]][y + dy[i]]
                    val d = sqrt(sqr(p0.x - p1.x) + sqr(p0.y - p1.y) + sqr(p0.z - p1.z))
                    p0.vz += K * (p1.z - p0.z) / d * DT
                    p0.vz *= 0.99f
                }
            }
        }

        for (y in 1 until N - 1) {
            for (x in 1 until N - 1) {
                points[x][y].z += points[x][y].vz
            }
        }
    }

    private fun getWaveHeight(x: Float, z: Float): Float {
        val waterX = (x + radius) / (2 * radius)
        val waterZ = (z + radius) / (2 * radius)
        val ix = ((waterX * (N - 1)).toInt().coerceIn(0, N - 1))
        val iz = ((waterZ * (N - 1)).toInt().coerceIn(0, N - 1))
        return points[ix][iz].z * 0.2f
    }

    private fun generateSpheres() {
        updateWaves()

        val innerVertices = mutableListOf<Float>()
        val innerNormals = mutableListOf<Float>()
        val innerIndices = mutableListOf<Short>()
        val outerVertices = mutableListOf<Float>()

        for (i in 0..stacks) {
            val phi = PI * i / stacks
            val sinPhi = sin(phi).toFloat()
            val cosPhi = cos(phi).toFloat()

            for (j in 0..slices) {
                val theta = 2 * PI * j / slices
                val sinTheta = sin(theta).toFloat()
                val cosTheta = cos(theta).toFloat()

                val xBase = sinPhi * cosTheta
                val yBase = cosPhi
                val zBase = sinPhi * sinTheta

                val waveHeight = getWaveHeight(xBase * radius, zBase * radius)

                val ix = xBase * innerRadius
                val iy = yBase * innerRadius
                val iz = zBase * innerRadius

                innerVertices.add(ix)
                innerVertices.add(iy)
                innerVertices.add(iz)

                innerNormals.add(xBase)
                innerNormals.add(yBase)
                innerNormals.add(zBase)

                val ox = xBase * radius
                val oy = yBase * radius + waveHeight
                val oz = zBase * radius

                outerVertices.add(ox)
                outerVertices.add(oy)
                outerVertices.add(oz)
            }
        }

        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val first = (i * (slices + 1) + j).toShort()
                val second = (first + slices + 1).toShort()

                innerIndices.add(first)
                innerIndices.add(second)
                innerIndices.add((first + 1).toShort())

                innerIndices.add(second)
                innerIndices.add((second + 1).toShort())
                innerIndices.add((first + 1).toShort())
            }
        }

        indexCount = innerIndices.size

        // Буфер внутренней сферы
        innerVertexBuffer = ByteBuffer.allocateDirect(innerVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(innerVertices.toFloatArray())
                position(0)
            }

        innerNormalBuffer = ByteBuffer.allocateDirect(innerNormals.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(innerNormals.toFloatArray())
                position(0)
            }

        innerIndexBuffer = ByteBuffer.allocateDirect(innerIndices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(innerIndices.toShortArray())
                position(0)
            }

        // Буфер внешней сферы (точки)
        outerVertexBuffer = ByteBuffer.allocateDirect(outerVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(outerVertices.toFloatArray())
                position(0)
            }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun initShaders() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        val vertexPointsShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderPointsCode)
        val fragmentPointsShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderPointsCode)
        mProgramPoints = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexPointsShader)
            GLES20.glAttachShader(it, fragmentPointsShader)
            GLES20.glLinkProgram(it)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.02f, 0.05f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        initWaterPhysics()
        initShaders()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        W = width.toFloat()
        H = height.toFloat()
    }

    private fun calcMatrix() {
        val ratio = W / H
        val k = 0.9f
        val left = -k * ratio
        val right = k * ratio
        val bottom = -k
        val top = k
        val near = 0.1f
        val far = 10.0f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, 30f, 1f, 0.5f, 0f)

        Matrix.setLookAtM(viewMatrix, 0, xCamera, yCamera, zCamera, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        generateSpheres()
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
        GLES20.glVertexAttribPointer(aVertexHandle, 3, GLES20.GL_FLOAT, false, 0, innerVertexBuffer)

        val aNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_normal")
        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, 0, innerNormalBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, innerIndexBuffer)

        GLES20.glDisableVertexAttribArray(aVertexHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)

        GLES20.glUseProgram(mProgramPoints)

        val uMvpPointsHandle = GLES20.glGetUniformLocation(mProgramPoints, "u_MVPMatrix")
        GLES20.glUniformMatrix4fv(uMvpPointsHandle, 1, false, modelViewProjectionMatrix, 0)

        val uColorHandle = GLES20.glGetUniformLocation(mProgramPoints, "uColor")
        GLES20.glUniform3f(uColorHandle, 0.4f, 0.7f, 1.0f)

        val aVertexPointsHandle = GLES20.glGetAttribLocation(mProgramPoints, "a_vertex")
        GLES20.glEnableVertexAttribArray(aVertexPointsHandle)
        GLES20.glVertexAttribPointer(aVertexPointsHandle, 3, GLES20.GL_FLOAT, false, 0, outerVertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, (stacks + 1) * (slices + 1))

        GLES20.glDisableVertexAttribArray(aVertexPointsHandle)
    }
}