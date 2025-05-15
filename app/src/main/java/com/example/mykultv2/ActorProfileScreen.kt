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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBack
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Composable
fun ActorProfileScreen(navController: NavHostController, actorId: String) {
    val json = Json { ignoreUnknownKeys = true }
    var actorDetails by remember { mutableStateOf<PersonDetails?>(null) }
    var actorMovies by remember { mutableStateOf<List<ActorMovie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(actorId) {
        scope.launch {
            try {
                val personJson = withContext(Dispatchers.IO) {
                    URL("https://api.themoviedb.org/3/person/$actorId?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=en-US").readText()
                }
                actorDetails = json.decodeFromString<PersonDetails>(personJson)

                val creditsJson = withContext(Dispatchers.IO) {
                    URL("https://api.themoviedb.org/3/person/$actorId/movie_credits?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=en-US").readText()
                }
                val creditsResponse = json.decodeFromString<ActorCreditsResponse>(creditsJson)
                actorMovies = creditsResponse.cast ?: emptyList()
            } catch (e: Exception) {
                error = "Error loading actor profile: ${e.message}"
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
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Actor Profile",
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
                actorDetails != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Name: ${actorDetails?.name ?: "N/A"}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        if (actorDetails != null) {
            if (actorMovies.isNotEmpty()) {
                item {
                    Text(
                        text = "Movies by Actor",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(actorMovies) { movie ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("filmDetail/${movie.id}")
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
                                model = "https://image.tmdb.org/t/p/w500${movie.poster_path}",
                                contentDescription = "Poster of ${movie.title}",
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
                                    text = movie.title ?: "N/A",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Release: ${movie.release_date ?: "N/A"}",
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
                                            val movieToFavorite = Movie(
                                                id = movie.id,
                                                title = movie.title ?: "N/A",
                                                release_date = movie.release_date ?: "N/A",
                                                poster_path = movie.poster_path
                                            )
                                            FavoritesManager.toggleFavoriteMovie(movieToFavorite)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (FavoritesManager.isMovieFavorited(
                                            Movie(
                                                id = movie.id,
                                                title = movie.title ?: "N/A",
                                                release_date = movie.release_date ?: "N/A",
                                                poster_path = movie.poster_path
                                            )
                                        )) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (FavoritesManager.isMovieFavorited(
                                            Movie(
                                                id = movie.id,
                                                title = movie.title ?: "N/A",
                                                release_date = movie.release_date ?: "N/A",
                                                poster_path = movie.poster_path
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

// Data Classes for Actor Profile and Credits
@Serializable
data class PersonDetails(
    val id: Int,
    val name: String
)

@Serializable
data class ActorCreditsResponse(
    val cast: List<ActorMovie>? = null
)

@Serializable
data class ActorMovie(
    val id: Int,
    val title: String? = null,
    val poster_path: String? = null,
    val release_date: String? = null
)