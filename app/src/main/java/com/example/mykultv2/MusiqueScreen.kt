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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.URL
import kotlin.random.Random



@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MusiqueScreen(navController: NavHostController) {
    var tracks by remember { mutableStateOf<List<Music>>(emptyList()) }
    var tracksError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var selectedGenre by remember { mutableStateOf<Genre?>(null) }
    var genres by remember { mutableStateOf<List<Genre>>(emptyList()) }
    val totalTracksToLoad = 100
    val tracksPerPage = 20
    val maxPages = totalTracksToLoad / tracksPerPage

    val json = Json { ignoreUnknownKeys = true }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        FavoritesManager.loadFavorites()
        genres = predefinedGenres
        Log.d("MusiqueScreen", "Predefined genres: ${genres.map { "${it.name} (ID: ${it.id})" }}")
    }

    suspend fun fetchTracks(genreId: Int? = null): List<Music> {
        return try {
            val genreQuery = if (genreId != null) "&genreId=$genreId" else "&genreId=14"
            val randomOffset = Random.nextInt(0, 1001 - tracksPerPage)
            val url = "https://itunes.apple.com/search?term=music&entity=song$genreQuery&limit=$tracksPerPage&offset=$randomOffset"
            Log.d("MusiqueScreen", "Fetching tracks from: $url")
            val tracksJson = withContext(Dispatchers.IO) {
                URL(url).readText()
            }
            Log.d("MusiqueScreen", "Tracks response: $tracksJson")
            val jsonObject = Json.parseToJsonElement(tracksJson).jsonObject
            val items = jsonObject["results"]?.jsonArray ?: return emptyList()

            val fetchedTracks = items.mapNotNull { item ->
                val id = item.jsonObject["trackId"]?.toString() ?: return@mapNotNull null
                val title = item.jsonObject["trackName"]?.toString()?.trim('"') ?: return@mapNotNull null
                val artist = item.jsonObject["artistName"]?.toString()?.trim('"') ?: "Unknown Artist"
                val cover = item.jsonObject["artworkUrl100"]?.toString()?.trim('"')?.replace("100x100", "300x300") ?: "https://via.placeholder.com/80"
                val releaseDate = item.jsonObject["releaseDate"]?.toString()?.trim('"')?.split("T")?.get(0) ?: "N/A"

                Log.d("MusiqueScreen", "Track: $title, Artist: $artist, Image URL: $cover")
                Music(
                    id = id,
                    trackName = title,
                    artistName = artist,
                    imageUrl = cover,
                    releaseDate = releaseDate
                )
            }

            Log.d("MusiqueScreen", "Fetched ${fetchedTracks.size} tracks for genreId ${genreId ?: "Default (14)"}")
            fetchedTracks
        } catch (e: Exception) {
            tracksError = "Error fetching music: ${e.message}"
            Log.e("MusiqueScreen", "Error fetching tracks: ${e.message}")
            emptyList()
        }
    }

    suspend fun refreshTracks() {
        isRefreshing = true
        tracks = emptyList()
        tracksError = null
        val newTracks = fetchTracks(selectedGenre?.id)
        tracks = newTracks
        isRefreshing = false
    }

    LaunchedEffect(selectedGenre) {
        currentPage = 1
        tracks = emptyList()
        tracksError = null
        isLoading = true
        val newTracks = fetchTracks(selectedGenre?.id)
        tracks = newTracks
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
                    val newTracks = fetchTracks(selectedGenre?.id)
                    tracks = tracks + newTracks
                    isLoading = false
                }
            }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { scope.launch { refreshTracks() } }
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
                text = "All Popular Music",
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
                if (tracksError != null) {
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
                                    text = tracksError ?: "Unknown error",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = "No music available for this genre.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(tracks) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { navController.navigate("musicDetail/${track.id}") },
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
                                    model = track.imageUrl,
                                    contentDescription = track.trackName,
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
                                                FavoritesManager.toggleFavoriteMusic(track)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (FavoritesManager.isMusicFavorited(track)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (FavoritesManager.isMusicFavorited(track)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                                    text = track.trackName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Release: ${track.releaseDate.take(4)}",
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