package com.velkov.mydb

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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