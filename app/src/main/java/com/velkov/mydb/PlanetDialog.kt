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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun PlanetDialog(
    planetName: String,
    textureResId: Int,
    radius: Float,
    infoText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlanetGLView(ctx, textureResId, radius)
                },
                modifier = Modifier.fillMaxSize()
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text("✕ Закрыть")
            }

            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                Text(
                    text = infoText,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

class PlanetGLView(
    context: Context,
    private val textureResId: Int,
    private val planetRadius: Float
) : GLSurfaceView(context) {
    private val renderer: PlanetRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = PlanetRenderer(context, textureResId, planetRadius)
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
}

class PlanetRenderer(
    private val context: Context,
    private val textureResId: Int,
    private val planetRadius: Float
) : GLSurfaceView.Renderer {

    private var texturedSphere: TexturedSphere? = null

    private var xCamera = 0f
    private var yCamera = 0f
    private var zCamera = 2.2f

    private var W = 0f
    private var H = 0f

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.02f, 0.03f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        texturedSphere = TexturedSphere(context, planetRadius, 40, 40, textureResId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        W = width.toFloat()
        H = height.toFloat()

        val ratio = W / H
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 1f, 10f)

        Matrix.setLookAtM(viewMatrix, 0, xCamera, yCamera, zCamera, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setIdentityM(modelMatrix, 0)

        Matrix.rotateM(modelMatrix, 0, 30f, 1f, 0.5f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        texturedSphere?.draw(mvpMatrix)
    }
}