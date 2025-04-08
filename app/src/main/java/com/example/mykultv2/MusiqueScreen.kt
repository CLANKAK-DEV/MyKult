package com.example.mykultv2

import android.util.Log
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.URL

@Composable
fun MusiqueScreen(navController: NavHostController) {
    var tracks by remember { mutableStateOf<List<Music1>>(emptyList()) }
    var tracksError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var selectedGenre by remember { mutableStateOf<Genre?>(null) } // Null means "All Genres"
    var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
    val totalTracksToLoad = 100
    val tracksPerPage = 20
    val maxPages = (totalTracksToLoad / tracksPerPage) + if (totalTracksToLoad % tracksPerPage > 0) 1 else 0

    val json = Json { ignoreUnknownKeys = true }
    val gridState = rememberLazyGridState()

    // Predefined list of genres with iTunes genre IDs
    val predefinedGenres = listOf(
        Genre(id = 14, name = "Pop"),
        Genre(id = 21, name = "Rock"),
        Genre(id = 11, name = "Jazz"),
        Genre(id = 6, name = "Country"),
        Genre(id = 7, name = "Electronic"),
        Genre(id = 18, name = "Hip-Hop/Rap"),
        Genre(id = 5, name = "Classical"),
        Genre(id = 20, name = "Alternative"),
        Genre(id = 15, name = "R&B/Soul"),
        Genre(id = 2, name = "Blues")
    )

    // Set genres on launch
    LaunchedEffect(Unit) {
        genres = predefinedGenres
        Log.d("MusiqueScreen", "Predefined genres: ${genres.map { "${it.name} (ID: ${it.id})" }}")
    }

    // Function to fetch tracks for a specific page and genre
    suspend fun fetchTracks(page: Int, genreId: Int? = null): List<Music1> {
        return try {
            val offset = (page - 1) * tracksPerPage
            val genreQuery = if (genreId != null) "&genreId=$genreId" else "&genreId=14" // Default to Pop (genreId 14)
            val url = "https://itunes.apple.com/search?term=music&entity=song$genreQuery&limit=$tracksPerPage&offset=$offset"
            Log.d("MusiqueScreen", "Fetching tracks from: $url") // Log the URL for debugging
            val tracksJson = withContext(Dispatchers.IO) {
                URL(url).readText()
            }
            Log.d("MusiqueScreen", "Tracks response: $tracksJson") // Log the tracks response
            val jsonObject = Json.parseToJsonElement(tracksJson).jsonObject
            val items = jsonObject["results"]?.jsonArray ?: return emptyList()

            val fetchedTracks = items.mapNotNull { item ->
                val id = item.jsonObject["trackId"]?.toString() ?: return@mapNotNull null // Get the track ID
                val title = item.jsonObject["trackName"]?.toString()?.trim('"') ?: return@mapNotNull null
                val artist = item.jsonObject["artistName"]?.toString()?.trim('"') ?: "Unknown Artist"
                val cover = item.jsonObject["artworkUrl100"]?.toString()?.trim('"')?.replace("100x100", "300x300") ?: "https://via.placeholder.com/80"
                val releaseDate = item.jsonObject["releaseDate"]?.toString()?.trim('"')?.split("T")?.get(0) ?: "N/A"

                Log.d("MusiqueScreen", "Track: $title, Artist: $artist, Image URL: $cover") // Log each track's image URL
                Music1(
                    id = id,
                    trackName = title,
                    artistName = artist,
                    imageUrl = cover,
                    releaseDate = releaseDate
                )
            }

            Log.d("MusiqueScreen", "Fetched ${fetchedTracks.size} tracks for page $page, genreId ${genreId ?: "Default (14)"}")
            fetchedTracks
        } catch (e: Exception) {
            tracksError = "Erreur lors du chargement des musiques: ${e.message}"
            Log.e("MusiqueScreen", "Error fetching tracks: ${e.message}")
            emptyList()
        }
    }

    // Reset and fetch tracks when genre changes
    LaunchedEffect(selectedGenre) {
        currentPage = 1
        tracks = emptyList()
        tracksError = null // Reset error message when changing genre
        isLoading = true
        val newTracks = fetchTracks(currentPage, selectedGenre?.id)
        tracks = newTracks
        isLoading = false
    }

    // Load more tracks when the user scrolls to the bottom
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
                val totalItems = gridState.layoutInfo.totalItemsCount

                if (lastVisibleItem >= totalItems - 4 && !isLoading && currentPage < maxPages) {
                    isLoading = true
                    currentPage++
                    val newTracks = fetchTracks(currentPage, selectedGenre?.id)
                    tracks = tracks + newTracks
                    isLoading = false
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Title
        Text(
            text = "Toute la Musique Populaire",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A00E0),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Category Selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All Genres" option
            item {
                CategoryChip(
                    category = "Tous les genres",
                    selected = selectedGenre == null,
                    onClick = { selectedGenre = null }
                )
            }

            // Genre chips
            items(genres) { genre ->
                CategoryChip(
                    category = genre.name,
                    selected = selectedGenre?.id == genre.id,
                    onClick = { selectedGenre = genre }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tracks Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (tracksError != null) {
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
                            text = tracksError ?: "Erreur inconnue",
                            fontSize = 16.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (tracks.isEmpty() && !isLoading && tracksError == null) {
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
                            text = "Aucune musique disponible pour ce genre.",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(tracks) { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clickable {
                            navController.navigate("musicDetail/${track.id}")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
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
                                model = track.imageUrl,
                                contentDescription = "Image de ${track.trackName}",
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
                                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                                    AsyncImagePainter.State.Empty -> Box(
                                        modifier = Modifier.matchParentSize().background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) { Text("Image indisponible", color = Color.White, fontSize = 12.sp) }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = track.trackName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Artiste: ${track.artistName}",
                                fontSize = 12.sp,
                                color = Color(0xFF757575),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sortie: ${track.releaseDate}",
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

@Serializable
data class GenreResponse1(val data: List<Genre>)



@Serializable
data class Music1(
    val id: String, // Updated to include id
    val trackName: String,
    val artistName: String,
    val imageUrl: String,
    val releaseDate: String
)