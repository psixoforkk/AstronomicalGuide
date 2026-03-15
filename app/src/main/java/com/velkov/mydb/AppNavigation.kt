package com.velkov.mydb

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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