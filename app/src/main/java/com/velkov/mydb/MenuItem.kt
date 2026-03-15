package com.velkov.mydb

sealed class MenuItem(val route: String, val title: String, val icon: Int) {
    object News : MenuItem("news", "Новости", android.R.drawable.ic_menu_agenda)
    object OpenGL : MenuItem("opengl", "OpenGL Задание", android.R.drawable.ic_menu_gallery)
}