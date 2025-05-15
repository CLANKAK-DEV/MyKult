package com.mykult.app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.Dp
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    var popularMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var popularBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var popularMusic by remember { mutableStateOf<List<Music>>(emptyList()) }
    var moviesError by remember { mutableStateOf<String?>(null) }
    var booksError by remember { mutableStateOf<String?>(null) }
    var musicError by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val json = Json { ignoreUnknownKeys = true }
    val scope = rememberCoroutineScope()

    fun fetchNewData() {
        scope.launch {
            moviesError = null
            booksError = null
            musicError = null

            try {
                val randomPage = Random.nextInt(1, 101)
                val moviesJson = withContext(Dispatchers.IO) {
                    URL("https://api.themoviedb.org/3/movie/popular?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=en-US&page=$randomPage").readText()
                }
                val moviesResponse = json.decodeFromString<TMDbResponse>(moviesJson)
                popularMovies = moviesResponse.results.take(5)
            } catch (e: Exception) {
                moviesError = "Movies error: ${e.message}"
            }

            try {
                val randomKeyword = listOf("fiction", "nonfiction", "mystery", "fantasy", "biography").random()
                val booksJson = withContext(Dispatchers.IO) {
                    URL("https://www.googleapis.com/books/v1/volumes?q=bestsellers+$randomKeyword&maxResults=5&langRestrict=en").readText()
                }
                val jsonObject = Json.parseToJsonElement(booksJson).jsonObject
                val items = jsonObject["items"]?.jsonArray ?: emptyList()

                popularBooks = items.mapNotNull { item ->
                    val id = item.jsonObject["id"]?.toString()?.trim('"') ?: return@mapNotNull null
                    val volumeInfo = item.jsonObject["volumeInfo"]?.jsonObject ?: return@mapNotNull null
                    val title = volumeInfo["title"]?.toString()?.trim('"') ?: return@mapNotNull null
                    val authors = volumeInfo["authors"]?.jsonArray?.joinToString(", ") { it.toString().trim('"') } ?: "Unknown author"
                    val imageLinks = volumeInfo["imageLinks"]?.jsonObject
                    val thumbnail = imageLinks?.get("thumbnail")?.toString()?.trim('"')?.replace("http://", "https://") ?: ""
                    val publishedDate = volumeInfo["publishedDate"]?.toString()?.trim('"') ?: "N/A"

                    Book(
                        id = id,
                        title = title,
                        author = authors,
                        imageUrl = thumbnail,
                        publishedDate = publishedDate
                    )
                }
            } catch (e: Exception) {
                booksError = "Books error: ${e.message}"
            }

            try {
                val randomGenre = listOf("pop", "rock", "jazz", "hiphop", "classical").random()
                val musicJson = withContext(Dispatchers.IO) {
                    URL("https://itunes.apple.com/search?term=$randomGenre&entity=song&limit=5").readText()
                }
                val jsonObject = Json.parseToJsonElement(musicJson).jsonObject
                val items = jsonObject["results"]?.jsonArray ?: emptyList()

                popularMusic = items.mapNotNull { item ->
                    val id = item.jsonObject["trackId"]?.toString() ?: return@mapNotNull null
                    val title = item.jsonObject["trackName"]?.toString()?.trim('"') ?: return@mapNotNull null
                    val artist = item.jsonObject["artistName"]?.toString()?.trim('"') ?: "Unknown artist"
                    val cover = item.jsonObject["artworkUrl100"]?.toString()?.trim('"')?.replace("100x100", "300x300") ?: "https://via.placeholder.com/80"
                    val releaseDate = item.jsonObject["releaseDate"]?.toString()?.trim('"')?.split("T")?.get(0) ?: "N/A"

                    Music(
                        id = id,
                        trackName = title,
                        artistName = artist,
                        imageUrl = cover,
                        releaseDate = releaseDate
                    )
                }
            } catch (e: Exception) {
                musicError = "Music error: ${e.message}"
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                fetchNewData()
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(Unit) {
        fetchNewData()
    }

    val cardElevation = 1.dp
    val cornerRadius = 8.dp
    val itemWidth = 120.dp
    val imageHeight = 140.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Popular Movies",
                    onClick = {
                        try {
                            navController.navigate("films")
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Navigation error to Films: ${e.message}")
                        }
                    }
                )
            }

            item {
                when {
                    moviesError != null -> ErrorCard(error = moviesError ?: "Unknown error")
                    popularMovies.isEmpty() && !isRefreshing -> LoadingCard(message = "Loading movies...")
                    else -> MediaRow(
                        items = popularMovies,
                        itemWidth = itemWidth,
                        imageHeight = imageHeight,
                        cornerRadius = cornerRadius,
                        cardElevation = cardElevation,
                        onItemClick = { movie -> navController.navigate("filmDetail/${movie.id}") },
                        renderItem = { movie ->
                            MediaCard(
                                imageUrl = if (movie.poster_path != null) "https://image.tmdb.org/t/p/w200${movie.poster_path}" else null,
                                title = movie.title,
                                subtitle = "Release: ${movie.release_date.take(4)}",
                                isFavorited = FavoritesManager.isMovieFavorited(movie),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteMovie(movie)
                                    }
                                }
                            )
                        }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Popular Books",
                    onClick = {
                        try {
                            navController.navigate("books")
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Navigation error to Books: ${e.message}")
                        }
                    }
                )
            }

            item {
                when {
                    booksError != null -> ErrorCard(error = booksError ?: "Unknown error")
                    popularBooks.isEmpty() && !isRefreshing -> LoadingCard(message = "Loading books...")
                    else -> MediaRow(
                        items = popularBooks,
                        itemWidth = itemWidth,
                        imageHeight = imageHeight,
                        cornerRadius = cornerRadius,
                        cardElevation = cardElevation,
                        onItemClick = { book -> navController.navigate("bookDetail/${book.id}") },
                        renderItem = { book ->
                            MediaCard(
                                imageUrl = book.imageUrl,
                                title = book.title,
                                subtitle = "Author: ${book.author}",
                                isFavorited = FavoritesManager.isBookFavorited(book),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteBook(book)
                                    }
                                }
                            )
                        }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Popular Music",
                    onClick = {
                        try {
                            navController.navigate("music")
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Navigation error to Music: ${e.message}")
                        }
                    }
                )
            }

            item {
                when {
                    musicError != null -> ErrorCard(error = musicError ?: "Unknown error")
                    popularMusic.isEmpty() && !isRefreshing -> LoadingCard(message = "Loading music...")
                    else -> MediaRow(
                        items = popularMusic,
                        itemWidth = itemWidth,
                        imageHeight = imageHeight,
                        cornerRadius = cornerRadius,
                        cardElevation = cardElevation,
                        onItemClick = { music -> navController.navigate("musicDetail/${music.id}") },
                        renderItem = { music ->
                            MediaCard(
                                imageUrl = music.imageUrl,
                                title = music.trackName,
                                subtitle = "Artist: ${music.artistName}",
                                isFavorited = FavoritesManager.isMusicFavorited(music),
                                onFavoriteClick = {
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteMusic(music)
                                    }
                                }
                            )
                        }
                    )
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

@Composable
fun SectionHeader(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = "View All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun <T> MediaRow(
    items: List<T>,
    itemWidth: Dp,
    imageHeight: Dp,
    cornerRadius: Dp,
    cardElevation: Dp,
    onItemClick: (T) -> Unit,
    renderItem: @Composable (T) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .width(itemWidth)
                    .shadow(
                        elevation = cardElevation,
                        shape = RoundedCornerShape(cornerRadius)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .clickable { onItemClick(item) },
                shape = RoundedCornerShape(cornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                renderItem(item)
            }
        }
    }
}

@Composable
fun MediaCard(
    imageUrl: String?,
    title: String,
    subtitle: String,
    isFavorited: Boolean,
    onFavoriteClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondary),
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
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
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onFavoriteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ErrorCard(error: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                text = error,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LoadingCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Data Classes
@Serializable
data class TMDbResponse(val results: List<Movie>)

@Serializable
data class Movie(
    val id: Int,
    val title: String,
    val release_date: String,
    val vote_average: Double? = null,
    val poster_path: String? = null
)

@Serializable
data class GoogleBooksResponse(val items: List<BookItem>)

@Serializable
data class BookItem(val volumeInfo: VolumeInfo)

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val imageUrl: String,
    val publishedDate: String
)

@Serializable
data class VolumeInfo(
    val title: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val imageLinks: ImageLinks? = null,
    val previewLink: String? = null,
    val infoLink: String? = null
)

@Serializable
data class ImageLinks(
    val thumbnail: String? = null
)

@Serializable
data class Music(
    val id: String,
    val trackName: String,
    val artistName: String,
    val imageUrl: String,
    val releaseDate: String
)