package com.velkov.mydb

import android.content.Context
import android.opengl.Matrix

class SolarSystem(private val context: Context) {

    private val sun: TexturedSphere
    private val planets: List<Planet>
    private var lastTime: Long = System.currentTimeMillis()

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    init {
        sun = TexturedSphere(context, 2.0f, 40, 40, R.drawable.sun_texture)

        planets = listOf(
            Planet("Меркурий", 0.3f, 4f, 0.08f, context, R.drawable.mercury_texture),
            Planet("Венера", 0.35f, 5.5f, 0.06f, context, R.drawable.venus_texture),
            Planet("Земля", 0.37f, 7f, 0.05f, context, R.drawable.earth_texture, true),
            Planet("Марс", 0.32f, 8.5f, 0.04f, context, R.drawable.mars_texture),
            Planet("Юпитер", 0.9f, 11f, 0.025f, context, R.drawable.jupiter_texture),
            Planet("Сатурн", 0.75f, 13.5f, 0.02f, context, R.drawable.saturn_texture),
            Planet("Уран", 0.65f, 16f, 0.015f, context, R.drawable.uranus_texture),
            Planet("Нептун", 0.64f, 18.5f, 0.012f, context, R.drawable.neptune_texture)
        )
    }

    fun update() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastTime) / 200.0f
        lastTime = currentTime

        planets.forEach { it.update(deltaTime) }
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        sun.draw(mvpMatrix)

        planets.forEach { planet ->
            planet.draw(viewMatrix, projectionMatrix)
        }
    }
}
