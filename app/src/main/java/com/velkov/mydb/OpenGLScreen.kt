package com.velkov.mydb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun OpenGLScreen() {
    val context = LocalContext.current

    var openGLView by remember { mutableStateOf<OpenGLView?>(null) }
    var showMoonDialog by remember { mutableStateOf(false) }
    var currentPlanetName by remember { mutableStateOf("") }

    var showPlanetDialog by remember { mutableStateOf(false) }
    var currentPlanetTexture by remember { mutableStateOf(R.drawable.earth_texture) }
    var currentPlanetRadius by remember { mutableStateOf(0.37f) }
    var currentPlanetInfo by remember { mutableStateOf("") }
    var showWaterDialog by remember { mutableStateOf(false) }

    val planetData = mapOf(
        "Меркурий" to Triple(R.drawable.mercury_texture, 0.3f, "Меркурий | Радиус: 2 440 км | Расстояние от Солнца: 57.9 млн км | Самая быстрая планета"),
        "Венера" to Triple(R.drawable.venus_texture, 0.35f, "Венера | Радиус: 6 052 км | Расстояние: 108.2 млн км | Самая горячая планета"),
        "Земля" to Triple(R.drawable.earth_texture, 0.37f, "Земля | Радиус: 6 371 км | Расстояние: 149.6 млн км | Наш дом"),
        "Марс" to Triple(R.drawable.mars_texture, 0.32f, "Марс | Радиус: 3 390 км | Расстояние: 227.9 млн км | Красная планета"),
        "Юпитер" to Triple(R.drawable.jupiter_texture, 0.8f, "Юпитер | Радиус: 69 911 км | Расстояние: 778.5 млн км | Самая большая планета"),
        "Сатурн" to Triple(R.drawable.saturn_texture, 0.65f, "Сатурн | Радиус: 58 232 км | Расстояние: 1.43 млрд км | Планета с кольцами"),
        "Уран" to Triple(R.drawable.uranus_texture, 0.55f, "Уран | Радиус: 25 362 км | Расстояние: 2.87 млрд км | Вращается на боку"),
        "Нептун" to Triple(R.drawable.neptune_texture, 0.54f, "Нептун | Радиус: 24 622 км | Расстояние: 4.5 млрд км | Самые сильные ветры")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenGLView(ctx).also { view ->
                    openGLView = view
                    view.setOnPlanetChangeListener(object : OnPlanetChangeListener {
                        override fun onPlanetChanged(index: Int, name: String) {
                            currentPlanetName = name
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth() .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { openGLView?.previousPlanet() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Влево")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        openGLView?.let { view ->
                            currentPlanetName = view.getCurrentPlanetName()
                            when (currentPlanetName) {
                                "Луна" -> showMoonDialog = true
                                "Нептун" -> showWaterDialog = true
                                else -> {
                                    val data = planetData[currentPlanetName]
                                    if (data != null) {
                                        currentPlanetTexture = data.first
                                        currentPlanetRadius = data.second
                                        currentPlanetInfo = data.third
                                        showPlanetDialog = true
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Информация о $currentPlanetName",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Инфо")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { openGLView?.nextPlanet() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Вправо")
                }
            }
        }
    }
    if (showMoonDialog) {
        MoonDialog(
            onDismiss = { showMoonDialog = false }
        )
    }

    if (showWaterDialog) {
        NeptuneDialog(
            onDismiss = { showWaterDialog = false }
        )
    }

    if (showPlanetDialog) {
        PlanetDialog(
            planetName = currentPlanetName,
            textureResId = currentPlanetTexture,
            radius = currentPlanetRadius,
            infoText = currentPlanetInfo,
            onDismiss = { showPlanetDialog = false }
        )
    }

}

interface OnPlanetChangeListener {
    fun onPlanetChanged(index: Int, name: String)
}

class OpenGLView(context: Context) : GLSurfaceView(context) {
    private val renderer: OpenGLRenderer

    private var planetChangeListener: OnPlanetChangeListener? = null

    init {
        setEGLContextClientVersion(2)
        renderer = OpenGLRenderer(context)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    fun setOnPlanetChangeListener(listener: OnPlanetChangeListener) {
        planetChangeListener = listener
        renderer.setOnPlanetChangeListener(listener)
    }

    fun getCurrentPlanetName(): String {
        return renderer.getCurrentPlanetName()
    }

    fun nextPlanet() {
        renderer.nextPlanet()
        requestRender()
    }

    fun previousPlanet() {
        renderer.previousPlanet()
        requestRender()
    }
}

class OpenGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var backgroundSquare: BackgroundSquare? = null
    private var solarSystem: SolarSystem? = null
    //private var cube: Cube? = null
    private var startTime: Long = 0
    private var selectionCube: Cube? = null

    private var selectedPlanetIndex = 0
    private var planetChangeListener: OnPlanetChangeListener? = null

    private val planetNames = listOf(
        "Меркурий", "Венера", "Земля", "Марс",
        "Юпитер", "Сатурн", "Уран", "Нептун", "Луна"
    )

    fun getCurrentPlanetName(): String {
        return planetNames[selectedPlanetIndex]
    }

    fun setOnPlanetChangeListener(listener: OnPlanetChangeListener) {
        planetChangeListener = listener
    }

    fun nextPlanet() {
        selectedPlanetIndex = (selectedPlanetIndex + 1) % planetNames.size
        planetChangeListener?.onPlanetChanged(selectedPlanetIndex, planetNames[selectedPlanetIndex])
    }

    fun previousPlanet() {
        selectedPlanetIndex = if (selectedPlanetIndex - 1 < 0) {
            planetNames.size - 1
        } else {
            selectedPlanetIndex - 1
        }
        planetChangeListener?.onPlanetChanged(selectedPlanetIndex, planetNames[selectedPlanetIndex])
    }

    private fun getPlanetPosition(index: Int): FloatArray {
        return solarSystem?.getPlanetPosition(index) ?: floatArrayOf(0f, 0f, 0f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        backgroundSquare = BackgroundSquare(context)

        //cube = Cube()

        solarSystem = SolarSystem(context)
        startTime = System.currentTimeMillis()

        selectionCube = Cube()

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 1f, 100f)

        Matrix.setLookAtM(viewMatrix, 0,0f, 15f, 25f,0f, 0f, 0f,0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -20f, -20f)
        Matrix.scaleM(modelMatrix, 0, 25f, 25f, 1f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        backgroundSquare?.draw(mvpMatrix)

        /*Matrix.setIdentityM(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, 0.5f, 0.5f, 0.5f)
        Matrix.rotateM(modelMatrix, 0, cubeRotation, 1f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        cube?.draw(mvpMatrix)*/

        solarSystem?.update()
        solarSystem?.draw(viewMatrix, projectionMatrix)

        drawSelectionCube()

        (gl as? GLSurfaceView)?.requestRender()
    }

    private fun drawSelectionCube() {

        val planetPos = getPlanetPosition(selectedPlanetIndex)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, planetPos[0], planetPos[1], planetPos[2])

        val planetRadius = solarSystem?.getPlanetRadius(selectedPlanetIndex) ?: 0.4f
        val cubeScale = planetRadius * 1.2f
        Matrix.scaleM(modelMatrix, 0, cubeScale, cubeScale, cubeScale)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        selectionCube?.draw(mvpMatrix)
    }
    companion object {
        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
    }

    private fun cos(angle: Float): Float = kotlin.math.cos(angle.toDouble()).toFloat()
    private fun sin(angle: Float): Float = kotlin.math.sin(angle.toDouble()).toFloat()
}


class BackgroundSquare(context: Context) {

    private val program: Int
    private val positionHandle: Int
    private val mvpMatrixHandle: Int
    private val textureHandle: Int
    private val textureCoordHandle: Int
    private val textureId: Int

    private val squareVertices = floatArrayOf(-1f, -1f, 0f, 1f, -1f, 0f, -1f,  1f, 0f, 1f,  1f, 0f)

    private val textureCoords = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)

    private val drawOrder = shortArrayOf(0, 1, 2, 1, 3, 2)

    private val vertexBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer
    private val indexBuffer: java.nio.ShortBuffer

    init {
        vertexBuffer = ByteBuffer.allocateDirect(squareVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(squareVertices)
                position(0)
            }

        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(textureCoords)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(drawOrder)
                position(0)
            }

        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """.trimIndent()

        val vertexShader = OpenGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = OpenGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        textureCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")

        textureId = loadTexture(context)
    }

    private fun loadTexture(context: Context): Int {

        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.galaxy)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        return textureIds[0]

    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }
}

class Cube {

    private val program: Int
    private val positionHandle: Int
    private val mvpMatrixHandle: Int
    private val colorHandle: Int

    private val cubeVertices = floatArrayOf(
        -1f, -1f,  1f,
        1f, -1f,  1f,
        -1f,  1f,  1f,
        1f,  1f,  1f,

        -1f, -1f, -1f,
        1f, -1f, -1f,
        -1f,  1f, -1f,
        1f,  1f, -1f,

        -1f, -1f, -1f,
        -1f, -1f,  1f,
        -1f,  1f, -1f,
        -1f,  1f,  1f,

        1f, -1f, -1f,
        1f, -1f,  1f,
        1f,  1f, -1f,
        1f,  1f,  1f,

        -1f,  1f, -1f,
        1f,  1f, -1f,
        -1f,  1f,  1f,
        1f,  1f,  1f,

        -1f, -1f, -1f,
        1f, -1f, -1f,
        -1f, -1f,  1f,
        1f, -1f,  1f
    )

    private val drawOrder = shortArrayOf(
        0, 1, 2, 1, 3, 2,
        4, 5, 6, 5, 7, 6,
        8, 9, 10, 9, 11, 10,
        12, 13, 14, 13, 15, 14,
        16, 17, 18, 17, 19, 18,
        20, 21, 22, 21, 23, 22
    )

    private val vertexBuffer: FloatBuffer
    private val indexBuffer: java.nio.ShortBuffer

    init {
        vertexBuffer = ByteBuffer.allocateDirect(cubeVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(cubeVertices)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(drawOrder)
                position(0)
            }

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

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glUniform4f(colorHandle, 0.3f, 0.6f, 1.0f, 0.3f)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

}