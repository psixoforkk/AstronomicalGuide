package com.velkov.mydb

sealed class MenuItem(val route: String, val title: String, val icon: Int) {
    object News : MenuItem("news", "Новости", android.R.drawable.ic_menu_agenda)
    object Task2 : MenuItem("task2", "Задание 2", android.R.drawable.ic_menu_edit)
}