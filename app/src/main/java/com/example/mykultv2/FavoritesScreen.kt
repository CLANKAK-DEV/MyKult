package com.example.mykultv2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.launch



@Composable
fun FavoritesScreen(navController: NavHostController) {
    val favoriteMovies by remember { derivedStateOf { FavoritesManager.favoriteMovies } }
    val favoriteBooks by remember { derivedStateOf { FavoritesManager.favoriteBooks } }
    val favoriteMusic by remember { derivedStateOf { FavoritesManager.favoriteMusic } }
    val allFavorites by remember { derivedStateOf { FavoritesManager.allFavorites } }

    LaunchedEffect(Unit) {
        FavoritesManager.loadFavorites()
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val scope = rememberCoroutineScope()

    val displayedFavorites: List<FavoriteItem> = when (selectedCategory) {
        "Movies" -> favoriteMovies.map { movie -> allFavorites.first { it.item == movie } }
        "Books" -> favoriteBooks.map { book -> allFavorites.first { it.item == book } }
        "Music" -> favoriteMusic.map { music -> allFavorites.first { it.item == music } }
        else -> allFavorites
    }

    val cardElevation = 1.dp
    val cornerRadius = 8.dp
    val itemWidth = 120.dp
    val imageHeight = 140.dp

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("All", "Movies", "Books", "Music").forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (displayedFavorites.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(cornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
                ) {
                    Text(
                        text = when (selectedCategory) {
                            "Movies" -> "No favorite movies"
                            "Books" -> "No favorite books"
                            "Music" -> "No favorite music"
                            else -> "No favorites"
                        },
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
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(displayedFavorites) { favoriteItem ->
                        val item = favoriteItem.item
                        when (item) {
                            is Movie -> MediaCard(
                                imageUrl = if (item.poster_path != null) "https://image.tmdb.org/t/p/w200${item.poster_path}" else null,
                                title = item.title,
                                subtitle = "Release: ${item.release_date.take(4)}",
                                isFavorited = FavoritesManager.isMovieFavorited(item),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteMovie(item)
                                    }
                                },
                                onClick = { navController.navigate("movieDetail/${item.id}") },
                                cardElevation = cardElevation,
                                cornerRadius = cornerRadius,
                                itemWidth = itemWidth,
                                imageHeight = imageHeight
                            )
                            is Book -> MediaCard(
                                imageUrl = item.imageUrl,
                                title = item.title,
                                subtitle = "Author: ${item.author}",
                                isFavorited = FavoritesManager.isBookFavorited(item),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteBook(item)
                                    }
                                },
                                onClick = { navController.navigate("bookDetail/${item.id}") },
                                cardElevation = cardElevation,
                                cornerRadius = cornerRadius,
                                itemWidth = itemWidth,
                                imageHeight = imageHeight
                            )
                            is Music -> MediaCard(
                                imageUrl = item.imageUrl,
                                title = item.trackName,
                                subtitle = "Artist: ${item.artistName}",
                                isFavorited = FavoritesManager.isMusicFavorited(item),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteMusic(item)
                                    }
                                },
                                onClick = { navController.navigate("musicDetail/${item.id}") },
                                cardElevation = cardElevation,
                                cornerRadius = cornerRadius,
                                itemWidth = itemWidth,
                                imageHeight = imageHeight
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaCard(
    imageUrl: String?,
    title: String,
    subtitle: String,
    isFavorited: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    cardElevation: Dp,
    cornerRadius: Dp,
    itemWidth: Dp,
    imageHeight: Dp
) {
    Card(
        modifier = Modifier
            .width(itemWidth)
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onClick() },
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondary),
                    contentScale = ContentScale.Crop
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Image error",
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> SubcomposeAsyncImageContent()
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onFavoriteClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}