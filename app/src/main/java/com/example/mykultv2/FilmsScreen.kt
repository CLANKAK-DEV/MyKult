package com.example.mykultv2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

// Main composable with navigation setup


@Composable
fun FilmsScreen(navController: NavHostController) {
    var allMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var moviesError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf<Genre?>(null) }
    val totalMoviesToLoad = 100
    val moviesPerPage = 20
    val maxPages = (totalMoviesToLoad / moviesPerPage) + if (totalMoviesToLoad % moviesPerPage > 0) 1 else 0

    val json = Json { ignoreUnknownKeys = true }
    val gridState = rememberLazyGridState()

    // Fetch genres
    LaunchedEffect(Unit) {
        try {
            val genresJson = withContext(Dispatchers.IO) {
                URL("https://api.themoviedb.org/3/genre/movie/list?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=fr-FR").readText()
            }
            val genresResponse = json.decodeFromString<GenreResponse>(genresJson)
            genres = genresResponse.genres
        } catch (e: Exception) {
            moviesError = "Erreur lors du chargement des genres: ${e.message}"
        }
    }

    suspend fun fetchMovies(page: Int, genreId: Int? = null): List<Movie> {
        return try {
            val genreQuery = if (genreId != null) "&with_genres=$genreId" else ""
            val moviesJson = withContext(Dispatchers.IO) {
                URL("https://api.themoviedb.org/3/discover/movie?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=fr-FR&page=$page$genreQuery").readText()
            }
            val moviesResponse = json.decodeFromString<TMDbResponse>(moviesJson)
            moviesResponse.results
        } catch (e: Exception) {
            moviesError = "Erreur lors du chargement des films: ${e.message}"
            emptyList()
        }
    }

    LaunchedEffect(selectedGenre) {
        currentPage = 1
        allMovies = emptyList()
        isLoading = true
        val movies = fetchMovies(currentPage, selectedGenre?.id)
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
                    val newMovies = fetchMovies(currentPage, selectedGenre?.id)
                    allMovies = allMovies + newMovies
                    isLoading = false
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Tous les Films Populaires",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A00E0),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedGenre == null,
                    onClick = { selectedGenre = null },
                    label = { Text("Tous les genres") },
                    modifier = Modifier.border(width = 0.dp, color = Color.Transparent),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4A00E0),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFE0E0E0),
                        labelColor = Color.Black
                    )
                )
            }

            items(genres.size) { index ->
                val genre = genres[index]
                FilterChip(
                    selected = selectedGenre?.id == genre.id,
                    onClick = { selectedGenre = genre },
                    label = { Text(genre.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4A00E0),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFE0E0E0),
                        labelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (moviesError != null) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = moviesError ?: "Erreur inconnue",
                            fontSize = 16.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(allMovies.size) { index ->
                val movie = allMovies[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clickable {
                            navController.navigate("filmDetail/${movie.id}")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ){
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SubcomposeAsyncImage(
                                model = if (movie.poster_path != null) "https://image.tmdb.org/t/p/w200${movie.poster_path}" else null,
                                contentDescription = "Affiche de ${movie.title}",
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Loading -> Box(
                                        modifier = Modifier.matchParentSize().background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }
                                    is AsyncImagePainter.State.Error -> Box(
                                        modifier = Modifier.matchParentSize().background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) { Text("Erreur", color = Color.White, fontSize = 12.sp) }
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = movie.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sortie: ${movie.release_date}",
                                fontSize = 12.sp,
                                color = Color(0xFF757575),
                                textAlign = TextAlign.Center
                            )
                        }

                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-12).dp, y = (-12).dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA500)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}



// Data classes remain the same
@Serializable
data class GenreResponse(val genres: List<Genre>)

@Serializable
data class Genre(
    val id: Int,
    val name: String
)

