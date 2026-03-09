package com.velkov.mydb

data class NewsModel(
    val id: Int,
    val title: String,
    val description: String,
    var likes: Int = 0
)