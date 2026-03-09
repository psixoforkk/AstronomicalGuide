package com.velkov.mydb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    var selectedItem by remember { mutableStateOf<MenuItem>(MenuItem.News) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Меню",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text(MenuItem.News.title) },
                        selected = selectedItem == MenuItem.News,
                        onClick = {
                            selectedItem = MenuItem.News
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = MenuItem.News.icon),
                                contentDescription = null
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text(MenuItem.Task2.title) },
                        selected = selectedItem == MenuItem.Task2,
                        onClick = {
                            selectedItem = MenuItem.Task2
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = MenuItem.Task2.icon),
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedItem.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_sort_alphabetically),
                                contentDescription = "Меню"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedItem) {
                    is MenuItem.News -> NewsScreen()
                    is MenuItem.Task2 -> Task2Screen()
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

@Composable
fun Task2Screen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Второе задание",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚡ В разработке ⚡",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}