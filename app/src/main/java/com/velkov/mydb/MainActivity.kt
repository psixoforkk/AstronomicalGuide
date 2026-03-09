package com.velkov.mydb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velkov.mydb.NewsModel
import com.velkov.mydb.NewsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NewsScreen()
                }
            }
        }
    }
}

@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel()) {
    val news by viewModel.displayedNews.collectAsState()

    LaunchedEffect(Unit) {
        while(true) {
            delay(5000L)
            viewModel.updateRandomNews()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(1.dp)
                ) {
                    NewsQuarter(
                        news = news[0],
                        onLikeClick = { viewModel.likeNews(0) }
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(1.dp)
                ) {
                    NewsQuarter(
                        news = news[1],
                        onLikeClick = { viewModel.likeNews(1) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(1.dp)
                ) {
                    NewsQuarter(
                        news = news[2],
                        onLikeClick = { viewModel.likeNews(2) }
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(1.dp)
                ) {
                    NewsQuarter(
                        news = news[3],
                        onLikeClick = { viewModel.likeNews(3) }
                    )
                }
            }
        }
    }
}

@Composable
fun NewsQuarter(news: NewsModel, onLikeClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(9f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = news.title,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.description,
                    fontSize = 14.sp,
                    maxLines = 4
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Лайки ${news.likes}",
                    fontSize = 14.sp,
                    color = Color.Red
                )
                Button(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .height(32.dp)
                        .width(64.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+", fontSize = 14.sp)
                }
            }
        }
    }
}