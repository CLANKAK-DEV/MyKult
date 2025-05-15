package com.mykult.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import kotlin.random.Random

@Serializable
data class GenreResponse(val genres: List<Genre>)

@Serializable
data class Genre(
    val id: Int,
    val name: String
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FilmsScreen(navController: NavHostController) {
    var allMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var moviesError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf<Genre?>(null) }
    val totalMoviesToLoad = 100
    val moviesPerPage = 20
    val maxPages = (totalMoviesToLoad / moviesPerPage) + if (totalMoviesToLoad % moviesPerPage > 0) 1 else 0

    val json = Json { ignoreUnknownKeys = true }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val genresJson = withContext(Dispatchers.IO) {
                URL("https://api.themoviedb.org/3/genre/movie/list?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=en-US").readText()
            }
            val genresResponse = json.decodeFromString<GenreResponse>(genresJson)
            genres = genresResponse.genres
        } catch (e: Exception) {
            moviesError = "Error loading genres: ${e.message}"
        }
    }

    suspend fun fetchMovies(genreId: Int? = null): List<Movie> {
        return try {
            val genreQuery = if (genreId != null) "&with_genres=$genreId" else ""
            val randomPage = Random.nextInt(1, 501)
            val moviesJson = withContext(Dispatchers.IO) {
                URL("https://api.themoviedb.org/3/discover/movie?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=en-US&page=$randomPage$genreQuery").readText()
            }
            val moviesResponse = json.decodeFromString<TMDbResponse>(moviesJson)
            moviesResponse.results
        } catch (e: Exception) {
            moviesError = "Error loading movies: ${e.message}"
            emptyList()
        }
    }

    suspend fun refreshMovies() {
        isRefreshing = true
        allMovies = emptyList()
        val newMovies = fetchMovies(selectedGenre?.id)
        allMovies = newMovies
        isRefreshing = false
    }

    LaunchedEffect(selectedGenre) {
        currentPage = 1
        allMovies = emptyList()
        isLoading = true
        val movies = fetchMovies(selectedGenre?.id)
        allMovies = movies
        isLoading = false
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
                val totalItems = gridState.layoutInfo.totalItemsCount

                if (lastVisibleItem >= totalItems - 4 && !isLoading && currentPage < maxPages) {
                    isLoading = true
                    currentPage++
                    val newMovies = fetchMovies(selectedGenre?.id)
                    allMovies = allMovies + newMovies
                    isLoading = false
                }
            }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { scope.launch { refreshMovies() } }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "All Popular Movies",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedGenre == null,
                        onClick = { selectedGenre = null },
                        label = { Text("All Genres", fontSize = 12.sp) },
                        modifier = Modifier.border(width = 0.dp, color = MaterialTheme.colorScheme.background),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                items(genres) { genre ->
                    FilterChip(
                        selected = selectedGenre?.id == genre.id,
                        onClick = { selectedGenre = genre },
                        label = { Text(genre.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (moviesError != null) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = moviesError ?: "Unknown error",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                items(allMovies) { movie ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { navController.navigate("filmDetail/${movie.id}") },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                SubcomposeAsyncImage(
                                    model = if (movie.poster_path != null) "https://image.tmdb.org/t/p/w200${movie.poster_path}" else null,
                                    contentDescription = movie.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.secondary),
                                    contentScale = ContentScale.Crop
                                ) {
                                    when (painter.state) {
                                        is AsyncImagePainter.State.Loading -> Box(
                                            modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.secondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        is AsyncImagePainter.State.Error -> Box(
                                            modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.secondary),
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

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)
                                        .clip(CircleShape)
                                        .clickable {
                                            scope.launch {
                                                FavoritesManager.toggleFavoriteMovie(movie)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (FavoritesManager.isMovieFavorited(movie)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (FavoritesManager.isMovieFavorited(movie)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                                    text = movie.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Release: ${movie.release_date.take(4)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.dp
                            )
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}