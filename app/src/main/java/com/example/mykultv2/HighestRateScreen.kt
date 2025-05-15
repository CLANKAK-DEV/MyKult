package com.mykult.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Composable
fun HighestRateScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var topRatedMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMoreMovies by remember { mutableStateOf(true) }

    LaunchedEffect(currentPage) {
        scope.launch {
            try {
                if (currentPage == 1) {
                    isLoading = true
                } else {
                    isLoadingMore = true
                }

                val moviesJson = withContext(Dispatchers.IO) {
                    URL("https://api.themoviedb.org/3/movie/top_rated?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=en-US&page=$currentPage").readText()
                }
                val moviesResponse = Json { ignoreUnknownKeys = true }.decodeFromString<TMDbResponse>(moviesJson)

                topRatedMovies = if (currentPage == 1) {
                    moviesResponse.results
                } else {
                    topRatedMovies + moviesResponse.results
                }

                hasMoreMovies = moviesResponse.results.size == 20
            } catch (e: Exception) {
                error = "Error loading top-rated movies: ${e.message}"
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Top Rated",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(topRatedMovies) { movie ->
                            TopRatedListItem(
                                item = movie,
                                onClick = { navController.navigate("filmDetail/${movie.id}") }, // Changed to "filmDetail"
                                isFavorited = { FavoritesManager.isMovieFavorited(movie) },
                                onFavoriteClick = {
                                    scope.launch { FavoritesManager.toggleFavoriteMovie(movie) }
                                }
                            )
                        }

                        if (hasMoreMovies) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .clickable { currentPage++ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Load More...",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopRatedListItem(
    item: Any,
    onClick: () -> Unit,
    isFavorited: () -> Boolean,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (item) {
                is Movie -> {
                    SubcomposeAsyncImage(
                        model = if (item.poster_path != null) "https://image.tmdb.org/t/p/w200${item.poster_path}" else null,
                        contentDescription = "Poster of ${item.title}",
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            is AsyncImagePainter.State.Error -> Box(
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
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                when (item) {
                    is Movie -> {
                        Text(
                            text = item.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Release: ${item.release_date.take(4)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFD700), // Gold color for star
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.vote_average?.let { "$it/10" } ?: "N/A"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = if (isFavorited()) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorited()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onFavoriteClick() }
            )
        }
    }
}

