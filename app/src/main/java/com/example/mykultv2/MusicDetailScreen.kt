package com.example.mykultv2

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Composable
fun MusicDetailScreen(navController: NavHostController, musicId: String) {
    val json = Json { ignoreUnknownKeys = true }
    var musicDetails by remember { mutableStateOf<MusicDetails?>(null) }
    var artistDetails by remember { mutableStateOf<ArtistOrAlbum?>(null) }
    var artistAlbums by remember { mutableStateOf<List<ArtistOrAlbum>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Fetch track details
    LaunchedEffect(musicId) {
        scope.launch {
            try {
                val detailsJson = withContext(Dispatchers.IO) {
                    URL("https://itunes.apple.com/lookup?id=$musicId").readText()
                }
                val response = json.decodeFromString<iTunesResponse>(detailsJson)
                musicDetails = response.results.firstOrNull()
            } catch (e: Exception) {
                error = "Erreur lors du chargement des détails: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Fetch artist details and albums using artistId
    LaunchedEffect(musicDetails) {
        musicDetails?.artistId?.let { artistId ->
            scope.launch {
                try {
                    val artistJson = withContext(Dispatchers.IO) {
                        URL("https://itunes.apple.com/lookup?id=$artistId&entity=album&limit=5").readText()
                    }
                    val response = json.decodeFromString<ArtistResponse>(artistJson)
                    // The first result should be the artist, followed by albums
                    artistDetails = response.results.firstOrNull { it.wrapperType == "artist" }
                    artistAlbums = response.results.filter { it.wrapperType == "collection" }.take(5)
                } catch (e: Exception) {
                    error = "Erreur lors du chargement du profil de l'artiste: ${e.message}"
                }
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
                    text = "Détails de la Musique",
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
                musicDetails != null -> {
                    // Cover Image
                    SubcomposeAsyncImage(
                        model = musicDetails?.artworkUrl100?.replace("100x100", "300x300"),
                        contentDescription = "Image de ${musicDetails?.trackName}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
                            is AsyncImagePainter.State.Error -> Text("Erreur de chargement de l'image")
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                }
            }
        }

        if (musicDetails != null) {
            item {
                // Track Name
                Text(
                    text = musicDetails?.trackName ?: "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                // Artist
                Text(
                    text = "Artiste",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = musicDetails?.artistName ?: "N/A",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                // Album and Release Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Album: ${musicDetails?.collectionName ?: "N/A"}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Sortie: ${musicDetails?.releaseDate?.split("T")?.get(0) ?: "N/A"}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            item {
                // Genre
                Text(
                    text = "Genre",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = musicDetails?.primaryGenreName ?: "N/A",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            // Artist Profile Section
            item {
                Text(
                    text = "Profil de l'Artiste",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (artistDetails != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                artistDetails?.artistId?.let { artistId ->
                                    navController.navigate("artistProfile/$artistId")
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Artist Name
                            Text(
                                text = "Nom: ${artistDetails?.artistName ?: "N/A"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Artist Genre
                            Text(
                                text = "Genre: ${artistDetails?.primaryGenreName ?: "N/A"}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Chargement du profil de l'artiste...",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }

            // Artist Albums Section
            if (artistAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "Albums de l'Artiste",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(artistAlbums) { album ->
                            Card(
                                modifier = Modifier
                                    .width(120.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    SubcomposeAsyncImage(
                                        model = album.artworkUrl100?.replace("100x100", "300x300"),
                                        contentDescription = "Image de ${album.collectionName}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = album.collectionName ?: "N/A",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Play Now Button
                val playLink = musicDetails?.trackViewUrl?.takeIf { it.isNotBlank() }
                    ?: "https://www.google.com/search?q=${Uri.encode(musicDetails?.trackName)}+${Uri.encode(musicDetails?.artistName)}+song"

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playLink))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Impossible d'ouvrir le lien: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A00E0),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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

// Data Classes for Music Details
@Serializable
data class iTunesResponse(
    val results: List<MusicDetails>
)

@Serializable
data class MusicDetails(
    val wrapperType: String? = null, // 👈 Add this line

    val trackId: Long? = null, // Made nullable to handle deserialization
    val trackName: String? = null, // Made nullable to handle deserialization
    val artistName: String,
    val artistId: Int? = null,
    val collectionName: String? = null,
    val releaseDate: String? = null,
    val primaryGenreName: String? = null,
    val artworkUrl100: String? = null,
    val trackViewUrl: String? = null
)

// Data Classes for Artist Profile and Albums
@Serializable
data class ArtistResponse(
    val results: List<ArtistOrAlbum>
)

@Serializable
data class ArtistOrAlbum(
    val wrapperType: String, // "artist" or "collection" (for albums)
    val artistId: Int? = null,
    val artistName: String? = null,
    val primaryGenreName: String? = null,
    val collectionId: Int? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null
)