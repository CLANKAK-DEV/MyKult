package com.mykult.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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

    LaunchedEffect(artistId) {
        scope.launch {
            try {
                val artistJson = withContext(Dispatchers.IO) {
                    URL("https://itunes.apple.com/lookup?id=$artistId&entity=album&limit=50").readText()
                }
                val artistResponse = json.decodeFromString<ArtistResponse>(artistJson)
                artistDetails = artistResponse.results.firstOrNull { it.wrapperType == "artist" }
                allAlbums = artistResponse.results.filter { it.wrapperType == "collection" }

                val tracksJson = withContext(Dispatchers.IO) {
                    URL("https://itunes.apple.com/lookup?id=$artistId&entity=song&limit=50").readText()
                }
                val tracksResponse = json.decodeFromString<iTunesResponse>(tracksJson)
                topTracks = tracksResponse.results.filter { it.wrapperType == "track" && it.trackId != null && it.trackName != null }

                val trackGenres = topTracks.mapNotNull { it.primaryGenreName }.toSet()
                val albumGenres = allAlbums.mapNotNull { it.primaryGenreName }.toSet()
                allGenres = trackGenres + albumGenres
            } catch (e: Exception) {
                error = "Error loading artist profile: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "Artist Profile",
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                artistDetails != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Name: ${artistDetails?.artistName ?: "N/A"}",
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

            if (topTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "All Tracks",
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubcomposeAsyncImage(
                                model = track.artworkUrl100?.replace("100x100", "300x300"),
                                contentDescription = "Artwork of ${track.trackName}",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentScale = ContentScale.Crop
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Loading -> CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    is AsyncImagePainter.State.Error -> Text(
                                        "Error",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = track.trackName ?: "N/A",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Album: ${track.collectionName ?: "N/A"}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        scope.launch {
                                            val music = Music(
                                                id = track.trackId?.toString() ?: "",
                                                trackName = track.trackName ?: "N/A",
                                                artistName = track.artistName ?: "N/A",
                                                imageUrl = track.artworkUrl100?.replace("100x100", "300x300") ?: "",
                                                releaseDate = track.releaseDate?.split("T")?.get(0) ?: "N/A"
                                            )
                                            FavoritesManager.toggleFavoriteMusic(music)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (FavoritesManager.isMusicFavorited(
                                            Music(
                                                id = track.trackId?.toString() ?: "",
                                                trackName = track.trackName ?: "N/A",
                                                artistName = track.artistName ?: "N/A",
                                                imageUrl = track.artworkUrl100?.replace("100x100", "300x300") ?: "",
                                                releaseDate = track.releaseDate?.split("T")?.get(0) ?: "N/A"
                                            )
                                        )) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (FavoritesManager.isMusicFavorited(
                                            Music(
                                                id = track.trackId?.toString() ?: "",
                                                trackName = track.trackName ?: "N/A",
                                                artistName = track.artistName ?: "N/A",
                                                imageUrl = track.artworkUrl100?.replace("100x100", "300x300") ?: "",
                                                releaseDate = track.releaseDate?.split("T")?.get(0) ?: "N/A"
                                            )
                                        )) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (allAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "All Albums",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(allAlbums) { album ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                album.collectionId?.let { id ->
                                    navController.navigate("albumDetail/$id")
                                }
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubcomposeAsyncImage(
                                model = album.artworkUrl100?.replace("100x100", "300x300"),
                                contentDescription = "Artwork of ${album.collectionName}",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentScale = ContentScale.Crop
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Loading -> CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    is AsyncImagePainter.State.Error -> Text(
                                        "Error",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = album.collectionName ?: "N/A",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        scope.launch {
                                            val albumAsMusic = Music(
                                                id = album.collectionId?.toString() ?: "",
                                                trackName = album.collectionName ?: "N/A",
                                                artistName = artistDetails?.artistName ?: "N/A",
                                                imageUrl = album.artworkUrl100?.replace("100x100", "300x300") ?: "",
                                                releaseDate = album.releaseDate?.split("T")?.get(0) ?: "N/A"
                                            )
                                            FavoritesManager.toggleFavoriteMusic(albumAsMusic)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (FavoritesManager.isMusicFavorited(
                                            Music(
                                                id = album.collectionId?.toString() ?: "",
                                                trackName = album.collectionName ?: "N/A",
                                                artistName = artistDetails?.artistName ?: "N/A",
                                                imageUrl = album.artworkUrl100?.replace("100x100", "300x300") ?: "",
                                                releaseDate = album.releaseDate?.split("T")?.get(0) ?: "N/A"
                                            )
                                        )) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (FavoritesManager.isMusicFavorited(
                                            Music(
                                                id = album.collectionId?.toString() ?: "",
                                                trackName = album.collectionName ?: "N/A",
                                                artistName = artistDetails?.artistName ?: "N/A",
                                                imageUrl = album.artworkUrl100?.replace("100x100", "300x300") ?: "",
                                                releaseDate = album.releaseDate?.split("T")?.get(0) ?: "N/A"
                                            )
                                        )) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Back",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}