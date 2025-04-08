package com.example.mykultv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CultureAppTheme {
                val navController = rememberNavController()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "CultureVibe",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    bottomBar = { CultureBottomNavigation(navController) }
                ) { innerPadding ->
                    MainScreen(navController, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(navController = navController, startDestination = "Home") {
            composable("Home") { HomeScreen(navController) }
            composable("Films") { FilmsScreen(navController) }
            composable("Livres") { BooksScreen(navController) }

            composable("Musique") { MusiqueScreen(navController) } // Pass navController
            composable("filmDetail/{filmId}") { backStackEntry ->
                val filmId = backStackEntry.arguments?.getString("filmId") ?: ""
                FilmDetailScreen(navController, filmId)
            }
            composable("bookDetail/{bookId}") { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                BookDetailScreen(navController, bookId)
            }
            composable("musicDetail/{musicId}") { backStackEntry ->
                val musicId = backStackEntry.arguments?.getString("musicId") ?: ""
                MusicDetailScreen(navController, musicId)
            }
            composable("artistProfile/{artistId}") { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                ArtistProfileScreen(navController, artistId)
            }
            composable("authorProfile/{authorName}") { backStackEntry ->
                val authorName = backStackEntry.arguments?.getString("authorName") ?: ""
                AuthorProfileScreen(navController, authorName)
            }
            composable("actorProfile/{actorId}") { backStackEntry ->
                val actorId = backStackEntry.arguments?.getString("actorId") ?: ""
                ActorProfileScreen(navController, actorId)
            }
        }
    }
}

@Composable
fun EmptyPage(pageName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$pageName",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun CultureBottomNavigation(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Accueil", R.drawable.ic_home, "Home"),
        BottomNavItem("Films", R.drawable.ic_movie, "Films"),
        BottomNavItem("Livres", R.drawable.ic_book, "Livres"),
        BottomNavItem("Musique", R.drawable.ic_music, "Musique")
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Color.Transparent)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00C4B4),
                    selectedTextColor = Color(0xFF00C4B4),
                    unselectedIconColor = Color(0xFF181818),
                    unselectedTextColor = Color(0xFF1B1B1E)
                )
            )
        }
    }
}

data class BottomNavItem(val label: String, val icon: Int, val route: String)
