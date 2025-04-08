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
import androidx.compose.material.icons.filled.ArrowBack
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

    // Fetch actor details and their movie credits
    LaunchedEffect(actorId) {
        scope.launch {
            try {
                // Fetch actor details
                val personJson = withContext(Dispatchers.IO) {
                    URL("https://api.themoviedb.org/3/person/$actorId?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=fr-FR").readText()
                }
                actorDetails = json.decodeFromString<PersonDetails>(personJson)

                // Fetch actor's movie credits
                val creditsJson = withContext(Dispatchers.IO) {
                    URL("https://api.themoviedb.org/3/person/$actorId/movie_credits?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=fr-FR").readText()
                }
                val creditsResponse = json.decodeFromString<ActorCreditsResponse>(creditsJson)
                actorMovies = creditsResponse.cast ?: emptyList()
            } catch (e: Exception) {
                error = "Erreur lors du chargement du profil de l'acteur: ${e.message}"
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
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Profil de l'Acteur",
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
                actorDetails != null -> {
                    // Actor Name
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nom: ${actorDetails?.name ?: "N/A"}",
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
            // All Films Section
            if (actorMovies.isNotEmpty()) {
                item {
                    Text(
                        text = "Films de l'Acteur",
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubcomposeAsyncImage(
                                model = "https://image.tmdb.org/t/p/w500${movie.poster_path}",
                                contentDescription = "Affiche de ${movie.title}",
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
                                    text = movie.title ?: "N/A",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sortie: ${movie.release_date ?: "N/A"}",
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