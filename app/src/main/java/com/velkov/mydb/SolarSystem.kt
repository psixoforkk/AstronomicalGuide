package com.velkov.mydb

import android.content.Context
import android.opengl.Matrix

class SolarSystem(private val context: Context) {

    private val sun: TexturedSphere
    private val planets: MutableList<Planet> = mutableListOf()
    private var lastTime: Long = System.currentTimeMillis()

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var earthPlanet: Planet? = null
    private var moon: Planet? = null

    init {
        sun = TexturedSphere(context, 2.0f, 40, 40, R.drawable.sun_texture)

        val mercury = Planet("Меркурий", 0.3f, 4f, 0.08f, context, R.drawable.mercury_texture)
        val venus = Planet("Венера", 0.35f, 5.5f, 0.06f, context, R.drawable.venus_texture)
        val earth = Planet("Земля", 0.37f, 7f, 0.05f, context, R.drawable.earth_texture)
        val mars = Planet("Марс", 0.32f, 8.5f, 0.04f, context, R.drawable.mars_texture)
        val jupiter = Planet("Юпитер", 0.9f, 11f, 0.025f, context, R.drawable.jupiter_texture)
        val saturn = Planet("Сатурн", 0.75f, 13.5f, 0.02f, context, R.drawable.saturn_texture)
        val uranus = Planet("Уран", 0.65f, 16f, 0.015f, context, R.drawable.uranus_texture)
        val neptune = Planet("Нептун", 0.64f, 18.5f, 0.1f, context, R.drawable.neptune_texture)

        earthPlanet = earth

        moon = Planet(
            name = "Луна",
            radius = 0.12f,
            distanceFromSun = 7f,
            speed = 0.2f,
            context = context,
            textureResId = R.drawable.moon_texture,
            isMoon = true
        )

        planets.addAll(listOf(mercury, venus, earth, mars, jupiter, saturn, uranus, neptune))
        moon?.let { planets.add(it) }
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
            if (planet.name == "Луна") {
                planet.drawAsMoon(viewMatrix, projectionMatrix, earthPlanet)
            } else {
                planet.draw(viewMatrix, projectionMatrix)
            }
        }
    }

    fun getPlanetAngle(index: Int): Float {
        return if (index in planets.indices) {
            planets[index].getOrbitAngle()
        } else 0f
    }

    fun getPlanetPosition(index: Int): FloatArray {

        if (index !in planets.indices) return floatArrayOf(0f, 0f, 0f)

        val planet = planets[index]

        return if (planet.name == "Луна") {
            // Для Луны возвращаем позицию относительно Земли
            earthPlanet?.let { earth ->
                val earthPos = getEarthPosition()
                val moonAngle = planet.getOrbitAngle()
                val moonDistance = 1.2f  // расстояние Луны от Земли
                val x = earthPos[0] + moonDistance * cos(moonAngle)
                val z = earthPos[2] + moonDistance * sin(moonAngle)
                floatArrayOf(x, 0f, z)
            } ?: floatArrayOf(0f, 0f, 0f)
        } else {
            val angle = planet.getOrbitAngle()
            val distance = planet.distanceFromSun
            floatArrayOf(distance * cos(angle), 0f, distance * sin(angle))
        }
    }
    private fun getEarthPosition(): FloatArray {
        val earth = planets.find { it.name == "Земля" }
        return earth?.let {
            val angle = it.getOrbitAngle()
            val distance = it.distanceFromSun
            floatArrayOf(
                distance * cos(angle),
                0f,
                distance * sin(angle)
            )
        } ?: floatArrayOf(0f, 0f, 0f)
    }

    fun getPlanetRadius(index: Int): Float {
        return if (index in planets.indices) { planets[index].radius } else 0.4f
    }

    fun getPlanetsCount(): Int = planets.size
    fun getPlanetName(index: Int): String {
        return if (index in planets.indices) { planets[index].name } else "Неизвестно"
    }

    private fun cos(angle: Float): Float = kotlin.math.cos(angle.toDouble()).toFloat()
    private fun sin(angle: Float): Float = kotlin.math.sin(angle.toDouble()).toFloat()
}
