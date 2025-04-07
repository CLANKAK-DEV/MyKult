package com.example.mykultv2

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import java.net.URL

@Composable
fun HomeScreen(navController: NavHostController) {
    var popularMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var popularBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var popularMusic by remember { mutableStateOf<List<Music>>(emptyList()) }
    var moviesError by remember { mutableStateOf<String?>(null) }
    var booksError by remember { mutableStateOf<String?>(null) }
    var musicError by remember { mutableStateOf<String?>(null) }

    val json = Json { ignoreUnknownKeys = true }

    LaunchedEffect(Unit) {
        try {
            val moviesJson = withContext(Dispatchers.IO) {
                URL("https://api.themoviedb.org/3/movie/popular?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=fr-FR&page=1").readText()
            }
            val moviesResponse = json.decodeFromString<TMDbResponse>(moviesJson)
            popularMovies = moviesResponse.results.take(5)
        } catch (e: Exception) {
            moviesError = "Erreur lors du chargement des films: ${e.message}"
        }

        try {
            val booksJson = withContext(Dispatchers.IO) {
                URL("https://www.googleapis.com/books/v1/volumes?q=bestsellers&maxResults=5&langRestrict=fr").readText()
            }
            val booksResponse = json.decodeFromString<GoogleBooksResponse>(booksJson)
            popularBooks = booksResponse.items.mapNotNull { item ->
                // Skip books with null titles
                if (item.volumeInfo.title == null) return@mapNotNull null
                Book(
                    title = item.volumeInfo.title,
                    author = item.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                    imageUrl = item.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://") ?: "",
                    publishedDate = item.volumeInfo.publishedDate ?: "N/A"
                )
            }
        } catch (e: Exception) {
            booksError = "Erreur lors du chargement des livres: ${e.message}"
        }

        try {
            val musicJson = withContext(Dispatchers.IO) {
                URL("https://api.deezer.com/chart/0/tracks?limit=5").readText()
            }
            val musicResponse = json.decodeFromString<DeezerChartResponse>(musicJson)
            popularMusic = musicResponse.data.map { track ->
                Music(
                    trackName = track.title,
                    artistName = track.artist.name,
                    imageUrl = track.album.cover_medium ?: "https://via.placeholder.com/80",
                    releaseDate = track.release_date ?: "N/A" // Map release_date to releaseDate
                )
            }
        } catch (e: Exception) {
            musicError = "Erreur lors du chargement de la musique: ${e.message}"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Movies Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Films Populaires",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A00E0)
                )
                TextButton(onClick = { navController.navigate("Films") }) {
                    Text(
                        text = "SEE ALL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A00E0)
                    )
                }
            }
        }
        if (moviesError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(text = moviesError ?: "Erreur inconnue", fontSize = 16.sp, color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            }
        } else if (popularMovies.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(text = "Chargement des films...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            // In HomeScreen, inside the LazyRow for movies
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(popularMovies.size) { index ->
                        val movie = popularMovies[index]
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {
                                    navController.navigate("filmDetail/${movie.id}")
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            // Rest of your Card content remains the same
                            Column(modifier = Modifier.padding(8.dp)) {
                                SubcomposeAsyncImage(
                                    model = if (movie.poster_path != null) "https://image.tmdb.org/t/p/w200${movie.poster_path}" else null,
                                    contentDescription = "Affiche de ${movie.title}",
                                    modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                ) {
                                    when (painter.state) {
                                        is AsyncImagePainter.State.Loading -> Box(modifier = Modifier.matchParentSize().background(Color.Gray), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                        is AsyncImagePainter.State.Error -> Box(modifier = Modifier.matchParentSize().background(Color.Gray), contentAlignment = Alignment.Center) { Text("Erreur", color = Color.White, fontSize = 12.sp) }
                                        else -> SubcomposeAsyncImageContent()
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = movie.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Sortie: ${movie.release_date.take(4)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color(0xFF4A00E0), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Books Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Livres Populaires", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A00E0))
                TextButton(onClick = { navController.navigate("Livres") }) {
                    Text(text = "SEE ALL", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4A00E0))
                }
            }
        }
        if (booksError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(text = booksError ?: "Erreur inconnue", fontSize = 16.sp, color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            }
        } else if (popularBooks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(text = "Chargement des livres...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            item {
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(popularBooks.size) { index ->
                        val book = popularBooks[index]
                        Card(
                            modifier = Modifier.width(150.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                SubcomposeAsyncImage(
                                    model = book.imageUrl,
                                    contentDescription = "Couverture de ${book.title}",
                                    modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                ) {
                                    when (painter.state) {
                                        is AsyncImagePainter.State.Loading -> Box(modifier = Modifier.matchParentSize().background(Color.Gray), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                        is AsyncImagePainter.State.Error -> Box(modifier = Modifier.matchParentSize().background(Color.Gray), contentAlignment = Alignment.Center) { Text("Erreur", color = Color.White, fontSize = 12.sp) }
                                        else -> SubcomposeAsyncImageContent()
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = book.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Sortie: ${book.publishedDate.take(4)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color(0xFF4A00E0), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Music Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Musique Populaire", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A00E0))
                TextButton(onClick = { navController.navigate("Musique") }) {
                    Text(text = "SEE ALL", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4A00E0))
                }
            }
        }
        if (musicError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(text = musicError ?: "Erreur inconnue", fontSize = 16.sp, color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            }
        } else if (popularMusic.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(text = "Chargement de la musique...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            item {
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(popularMusic.size) { index ->
                        val music = popularMusic[index]
                        Card(
                            modifier = Modifier.width(150.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                SubcomposeAsyncImage(
                                    model = music.imageUrl,
                                    contentDescription = "Image de ${music.trackName}",
                                    modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                ) {
                                    when (painter.state) {
                                        is AsyncImagePainter.State.Loading -> Box(modifier = Modifier.matchParentSize().background(Color.Gray), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                        is AsyncImagePainter.State.Error -> Box(modifier = Modifier.matchParentSize().background(Color.Gray), contentAlignment = Alignment.Center) { Text("Erreur", color = Color.White, fontSize = 12.sp) }
                                        else -> SubcomposeAsyncImageContent()
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = music.trackName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Artiste: ${music.artistName}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color(0xFF4A00E0), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data Classes
@Serializable
data class TMDbResponse(val results: List<Movie>)

@Serializable
data class Movie(
    val id: Int, // Add this field
    val title: String,
    val release_date: String,
    val poster_path: String? = null
)

@Serializable
data class GoogleBooksResponse(val items: List<BookItem>)

@Serializable
data class BookItem(val volumeInfo: VolumeInfo)

@Serializable
data class VolumeInfo(
    val title: String?, // Made nullable to handle missing titles
    val authors: List<String>? = null,
    val imageLinks: ImageLinks? = null,
    val publishedDate: String? = null
)

@Serializable
data class ImageLinks(
    val thumbnail: String? = null
)

@Serializable
data class Book(
    val title: String,
    val author: String,
    val imageUrl: String,
    val publishedDate: String
)

@Serializable
data class DeezerChartResponse(val data: List<DeezerTrack>)

@Serializable
data class DeezerTrack(
    val title: String,
    val artist: DeezerArtist,
    val album: DeezerAlbum,
    val release_date: String? = null // Added to match the API field
)

@Serializable
data class DeezerArtist(
    val name: String
)

@Serializable
data class DeezerAlbum(
    val cover_medium: String? = null
)



@Serializable
data class Music(
    val trackName: String,
    val artistName: String,
    val imageUrl: String,
    val releaseDate: String // Added to match the parsing logic
)