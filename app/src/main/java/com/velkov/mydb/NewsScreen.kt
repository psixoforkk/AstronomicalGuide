package com.velkov.mydb

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val news by viewModel.displayedNews.collectAsState()

    LaunchedEffect(Unit) {
        while(true) {
            delay(5005L)
            viewModel.updateRandomNews()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(0.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)) {
                    NewsQuarter(
                        news = news[0],
                        onLikeClick = { viewModel.likeNews(0) }
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)) {
                    NewsQuarter(
                        news = news[1],
                        onLikeClick = { viewModel.likeNews(1) }
                    )
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)) {
                    NewsQuarter(
                        news = news[2],
                        onLikeClick = { viewModel.likeNews(2) }
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)) {
                    NewsQuarter(
                        news = news[3],
                        onLikeClick = { viewModel.likeNews(3) }
                    )
                }
            }
        }
    }
}