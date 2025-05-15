package com.mykult.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.launch
@Composable
fun RecentlyWatchedScreen(navController: NavHostController) {
    val recentlyWatched by remember { derivedStateOf { RecentlyWatchedManager.recentlyWatched } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        RecentlyWatchedManager.loadRecentlyWatched()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Récemment Consultés",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (recentlyWatched.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = "Aucun élément récemment consulté",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(recentlyWatched) { recentlyWatchedItem ->
                        val item = recentlyWatchedItem.item
                        when (item) {
                            is Movie -> MediaCard(
                                imageUrl = if (item.poster_path != null) "https://image.tmdb.org/t/p/w200${item.poster_path}" else null,
                                title = item.title,
                                subtitle = "Sortie: ${item.release_date.take(4)}",
                                isFavorited = FavoritesManager.isMovieFavorited(item),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteMovie(item)
                                    }
                                },
                                onClick = { navController.navigate("filmDetail/${item.id}") },
                                cardElevation = 1.dp,
                                cornerRadius = 8.dp,
                                itemWidth = 120.dp,
                                imageHeight = 140.dp
                            )
                            is Book -> MediaCard(
                                imageUrl = item.imageUrl,
                                title = item.title,
                                subtitle = "Auteur: ${item.author}",
                                isFavorited = FavoritesManager.isBookFavorited(item),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteBook(item)
                                    }
                                },
                                onClick = { navController.navigate("bookDetail/${item.id}") },
                                cardElevation = 1.dp,
                                cornerRadius = 8.dp,
                                itemWidth = 120.dp,
                                imageHeight = 140.dp
                            )
                            is Music -> MediaCard(
                                imageUrl = item.imageUrl,
                                title = item.trackName,
                                subtitle = "Artiste: ${item.artistName}",
                                isFavorited = FavoritesManager.isMusicFavorited(item),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteMusic(item)
                                    }
                                },
                                onClick = { navController.navigate("musicDetail/${item.id}") },
                                cardElevation = 1.dp,
                                cornerRadius = 8.dp,
                                itemWidth = 120.dp,
                                imageHeight = 140.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
