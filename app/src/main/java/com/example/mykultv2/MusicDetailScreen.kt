package com.mykult.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    var isFavorited by remember { mutableStateOf(false) }

    LaunchedEffect(musicId) {
        scope.launch {
            try {
                val detailsJson = withContext(Dispatchers.IO) {
                    URL("https://itunes.apple.com/lookup?id=$musicId").readText()
                }
                val response = json.decodeFromString<iTunesResponse>(detailsJson)
                musicDetails = response.results.firstOrNull()

                musicDetails?.let { details ->
                    val music = Music(
                        id = details.trackId?.toString() ?: "",
                        trackName = details.trackName ?: "Unknown Track",
                        artistName = details.artistName,
                        imageUrl = details.artworkUrl100?.replace("100x100", "300x300") ?: "",
                        releaseDate = details.releaseDate?.split("T")?.get(0) ?: "N/A"
                    )
                    RecentlyWatchedManager.addRecentlyWatchedMusic(music)
                    isFavorited = FavoritesManager.isMusicFavorited(music)
                }
            } catch (e: Exception) {
                error = "Error loading details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(musicDetails) {
        musicDetails?.artistId?.let { artistId ->
            scope.launch {
                try {
                    val artistJson = withContext(Dispatchers.IO) {
                        URL("https://itunes.apple.com/lookup?id=$artistId&entity=album&limit=5").readText()
                    }
                    val response = json.decodeFromString<ArtistResponse>(artistJson)
                    artistDetails = response.results.firstOrNull { it.wrapperType == "artist" }
                    artistAlbums = response.results.filter { it.wrapperType == "collection" }.take(5)
                } catch (e: Exception) {
                    error = "Error loading artist profile: ${e.message}"
                }
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.popBackStack() },
                    tint = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Music Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Row {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                musicDetails?.let { details ->
                                    val music = Music(
                                        id = details.trackId?.toString() ?: "",
                                        trackName = details.trackName ?: "Unknown Track",
                                        artistName = details.artistName,
                                        imageUrl = details.artworkUrl100?.replace("100x100", "300x300") ?: "",
                                        releaseDate = details.releaseDate?.split("T")?.get(0) ?: "N/A"
                                    )
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteMusic(music)
                                        isFavorited = FavoritesManager.isMusicFavorited(music)
                                    }
                                }
                            },
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                musicDetails?.let { details ->
                                    shareMusic(context, details.trackName ?: "Unknown Track", details.trackId?.toString() ?: "")
                                }
                            },
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
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
                musicDetails != null -> {
                    SubcomposeAsyncImage(
                        model = musicDetails?.artworkUrl100?.replace("100x100", "300x300"),
                        contentDescription = "Artwork of ${musicDetails?.trackName}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            is AsyncImagePainter.State.Error -> Text("Error loading image", color = MaterialTheme.colorScheme.onSecondary)
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                }
            }
        }

        if (musicDetails != null) {
            item {
                Text(
                    text = musicDetails?.trackName ?: "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Text(
                    text = "Artist",
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
                        text = "Release: ${musicDetails?.releaseDate?.split("T")?.get(0) ?: "N/A"}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            item {
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

            item {
                Text(
                    text = "Artist Profile",
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
                            Text(
                                text = "Name: ${artistDetails?.artistName ?: "N/A"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Genre: ${artistDetails?.primaryGenreName ?: "N/A"}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Loading artist profile...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            if (artistAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "Artist Albums",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(artistAlbums) { album ->
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        album.collectionId?.let { albumId ->
                                            navController.navigate("albumDetail/$albumId")
                                        }
                                    },
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
                                            model = album.artworkUrl100?.replace("100x100", "300x300"),
                                            contentDescription = "Artwork of ${album.collectionName}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.secondary),
                                            contentScale = ContentScale.Crop
                                        ) {
                                            when (painter.state) {
                                                is AsyncImagePainter.State.Loading -> CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                is AsyncImagePainter.State.Error -> Text(
                                                    "Error",
                                                    color = MaterialTheme.colorScheme.onSecondary,
                                                    fontSize = 12.sp
                                                )
                                                else -> SubcomposeAsyncImageContent()
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = album.collectionName ?: "N/A",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Release: ${album.releaseDate?.split("T")?.get(0)?.substring(0, minOf(4, album.releaseDate?.split("T")?.get(0)?.length ?: 0)) ?: "N/A"}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
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
                                "Unable to open link: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
        }
    }
}

// Helper function to share music details
private fun shareMusic(context: Context, trackName: String, trackId: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out this song!")
        putExtra(Intent.EXTRA_TEXT, "I'm listening to $trackName! Check it out: https://music.apple.com/us/song/$trackId")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share $trackName"))
}

// Data Classes for Music Details
@Serializable
data class iTunesResponse(
    val results: List<MusicDetails>
)

@Serializable
data class MusicDetails(
    val wrapperType: String? = null,
    val trackId: Long? = null,
    val trackName: String? = null,
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
    val wrapperType: String,
    val artistId: Int? = null,
    val artistName: String? = null,
    val primaryGenreName: String? = null,
    val collectionId: Int? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val releaseDate: String? = null
)