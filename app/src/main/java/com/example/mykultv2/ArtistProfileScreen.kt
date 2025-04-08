package com.example.mykultv2

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.serialization.json.Json
import java.net.URL

// ArtistProfileScreen Composable

// ArtistProfileScreen Composable
@Composable
fun ArtistProfileScreen(navController: NavHostController, artistId: String) {
    val json = Json { ignoreUnknownKeys = true }
    var artistDetails by remember { mutableStateOf<ArtistOrAlbum?>(null) }
    var topTracks by remember { mutableStateOf<List<MusicDetails>>(emptyList()) }
    var allAlbums by remember { mutableStateOf<List<ArtistOrAlbum>>(emptyList()) }
    var allGenres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fetch artist details, top tracks, and albums
    LaunchedEffect(artistId) {
        scope.launch {
            try {
                // Fetch artist details and albums
                val artistJson = withContext(Dispatchers.IO) {
                    URL("https://itunes.apple.com/lookup?id=$artistId&entity=album&limit=50").readText()
                }
                val artistResponse = json.decodeFromString<ArtistResponse>(artistJson)
                artistDetails = artistResponse.results.firstOrNull { it.wrapperType == "artist" }
                allAlbums = artistResponse.results.filter { it.wrapperType == "collection" }

                // Fetch all tracks (increase limit to get more tracks)
                val tracksJson = withContext(Dispatchers.IO) {
                    URL("https://itunes.apple.com/lookup?id=$artistId&entity=song&limit=50").readText()
                }
                val tracksResponse = json.decodeFromString<iTunesResponse>(tracksJson)
                // Filter only tracks with wrapperType "track"
                topTracks = tracksResponse.results.filter { it.wrapperType == "track" && it.trackId != null && it.trackName != null }

                // Extract all genres from tracks and albums
                val trackGenres = topTracks.mapNotNull { it.primaryGenreName }.toSet()
                val albumGenres = allAlbums.mapNotNull { it.primaryGenreName }.toSet()
                allGenres = trackGenres + albumGenres
            } catch (e: Exception) {
                error = "Erreur lors du chargement du profil de l'artiste: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Top Bar with Back Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Retour",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Profil de l'Artiste",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Erreur inconnue",
                            color = Color.Red
                        )
                    }
                }
                artistDetails != null -> {
                    // Artist Name with "Nom: " prefix at the top
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nom: ${artistDetails?.artistName ?: "N/A"}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        if (artistDetails != null) {
            // All Genres Section
            if (allGenres.isNotEmpty()) {
                item {
                    Text(
                        text = "Genres",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = allGenres.joinToString(", "),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            // All Tracks Section
            if (topTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Tous les Morceaux",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(topTracks) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                track.trackId?.let { id ->
                                    navController.navigate("musicDetail/$id")
                                }
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubcomposeAsyncImage(
                                model = track.artworkUrl100?.replace("100x100", "300x300"),
                                contentDescription = "Image de ${track.trackName}",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
                                    is AsyncImagePainter.State.Error -> Text("Erreur")
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = track.trackName ?: "N/A",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Album: ${track.collectionName ?: "N/A"}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Genre: ${track.primaryGenreName ?: "N/A"}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // All Albums Section
            if (allAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "Tous les Albums",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(allAlbums) { album ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubcomposeAsyncImage(
                                model = album.artworkUrl100?.replace("100x100", "300x300"),
                                contentDescription = "Image de ${album.collectionName}",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
                                    is AsyncImagePainter.State.Error -> Text("Erreur")
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = album.collectionName ?: "N/A",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                // Back Button
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFF4A00E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Retour",
                        fontSize = 16.sp,
                        color = Color(0xFF4A00E0)
                    )
                }
            }
        }
    }
}