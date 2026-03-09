package com.velkov.mydb

import androidx.lifecycle.ViewModel
import com.velkov.mydb.NewsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NewsViewModel : ViewModel() {

    private val newsTemplates = listOf(
        NewsModel(1, "Открытие экзопланеты", "Обнаружена новая планета в системе TRAPPIST-1", 0),
        NewsModel(2, "Солнечное затмение", "Завтра ожидается частичное солнечное затмение", 0),
        NewsModel(3, "Марсоход Perseverance", "Найдены следы древней жизни на Марсе", 0),
        NewsModel(4, "Млечный путь", "Получено новое фото центра галактики", 0),
        NewsModel(5, "Спутник Юпитера", "На Европе обнаружены гейзеры", 0),
        NewsModel(6, "Звездопад", "Этой ночью пик метеорного потока Персеиды", 0),
        NewsModel(7, "Телескоп Webb", "Получены новые снимки туманности Ориона", 0),
        NewsModel(8, "Лунная миссия", "NASA анонсировало новую миссию на Луну", 0),
        NewsModel(9, "Черная дыра", "Зафиксировано слияние черных дыр", 0),
        NewsModel(10, "Космическая станция", "К МКС отправился новый экипаж", 0)
    )

    private val likesMap = mutableMapOf<Int, Int>()

    private val _displayedNews = MutableStateFlow(
        newsTemplates.shuffled().take(4).map { template ->
            template.copy(likes = likesMap[template.id] ?: 0)
        }
    )
    val displayedNews: StateFlow<List<NewsModel>> = _displayedNews.asStateFlow()

    fun likeNews(index: Int) {
        val currentList = _displayedNews.value.toMutableList()
        val news = currentList[index]

        val currentLikes = (likesMap[news.id] ?: 0) + 1
        likesMap[news.id] = currentLikes

        currentList[index] = news.copy(likes = currentLikes)
        _displayedNews.value = currentList
    }

    fun updateRandomNews() {
        val currentList = _displayedNews.value.toMutableList()
        val randomIndex = (0..3).random()

        val currentIds = currentList.map { it.id }

        val availableNews = newsTemplates.filter { it.id !in currentIds }

        if (availableNews.isNotEmpty()) {
            val randomTemplate = availableNews.random()

            val savedLikes = likesMap[randomTemplate.id] ?: 0
            currentList[randomIndex] = randomTemplate.copy(likes = savedLikes)
            _displayedNews.value = currentList
        }
    }
}