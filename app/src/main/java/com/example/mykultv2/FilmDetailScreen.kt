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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Locale

@Composable
fun FilmDetailScreen(navController: NavHostController, filmId: String) {
    val json = Json { ignoreUnknownKeys = true }
    var movieDetails by remember { mutableStateOf<MovieDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(filmId) {
        scope.launch {
            try {
                if (!filmId.matches(Regex("\\d+"))) {
                    throw IllegalArgumentException("ID du film invalide : $filmId. Un ID numérique est requis.")
                }

                val detailsJson = withContext(Dispatchers.IO) {
                    URL("https://api.themoviedb.org/3/movie/$filmId?api_key=2e8e56d097cbdfb2bc76d988a80ab8fe&language=fr-FR&append_to_response=credits,watch/providers").readText()
                }
                movieDetails = json.decodeFromString<MovieDetails>(detailsJson)
            } catch (e: Exception) {
                error = "Erreur lors du chargement des détails: ${e.message}"
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
                    text = "Détails du Film",
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
                movieDetails != null -> {
                    // Poster Image
                    SubcomposeAsyncImage(
                        model = "https://image.tmdb.org/t/p/w500${movieDetails?.poster_path}",
                        contentDescription = "Affiche de ${movieDetails?.title}",
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

        if (movieDetails != null) {
            item {
                // Title
                Text(
                    text = movieDetails?.title ?: "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                // Release Date and Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sortie: ${movieDetails?.release_date ?: "N/A"}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Durée: ${movieDetails?.runtime?.let { "${it / 60}h ${it % 60}m" } ?: "N/A"}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            item {
                // Genres
                Text(
                    text = "Genres",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movieDetails?.genres?.joinToString(", ") { it.name } ?: "N/A",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                // Actors/Cast
                Text(
                    text = "Acteurs principaux",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(movieDetails?.credits?.cast?.take(5) ?: emptyList()) { actor ->
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SubcomposeAsyncImage(
                                model = "https://image.tmdb.org/t/p/w200${actor.profile_path}",
                                contentDescription = "Photo de ${actor.name}",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Error -> Text("Pas d'image")
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Text(
                                text = actor.name,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                // Description
                Text(
                    text = "Description",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Text(
                    text = movieDetails?.overview ?: "Pas de description disponible",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                // Production Companies
                Text(
                    text = "Sociétés de production",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movieDetails?.production_companies?.joinToString(", ") { it.name } ?: "N/A",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                // Budget
                Text(
                    text = "Budget",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                movieDetails?.let { details ->
                    Text(
                        text = if (details.budget != null && details.budget > 0) {
                            "${details.budget / 1_000_000}M $"
                        } else {
                            "N/A"
                        },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            item {
                // Watch Link
                val userRegion = Locale.getDefault().country // e.g., "FR", "US"
                val watchLink = movieDetails?.let { details ->
                    if (details.watch_providers?.results?.containsKey(userRegion) == true) {
                        details.watch_providers.results[userRegion]?.link?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    } ?: details.homepage?.takeIf { it.isNotBlank() }
                    ?: "https://www.justwatch.com/fr/recherche?q=${Uri.encode(details.title)}"
                } ?: "https://www.justwatch.com/fr/recherche?q="

                // Debug the watch link
                println("Watch Link: $watchLink")

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(watchLink))
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
                        text = "Watch Now",
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

// Updated Data Classes for Movie Details
@Serializable
data class MovieDetails(
    val id: Int,
    val title: String,
    val poster_path: String?,
    val overview: String?,
    val release_date: String?,
    val runtime: Int? = null,
    val genres: List<Genre>? = null,
    val production_companies: List<ProductionCompany>? = null,
    val budget: Long? = null,
    val homepage: String? = null,
    val credits: Credits? = null,
    val watch_providers: WatchProviders? = null
)

@Serializable
data class ProductionCompany(
    val id: Int,
    val name: String
)

@Serializable
data class WatchProviders(
    val results: Map<String, CountryProviders>? = null
)

@Serializable
data class CountryProviders(
    val link: String? = null,
    val flatrate: List<Provider>? = null,
    val buy: List<Provider>? = null,
    val rent: List<Provider>? = null
)

@Serializable
data class Provider(
    val provider_name: String,
    val provider_id: Int,
    val logo_path: String? = null
)

@Serializable
data class Credits(
    val cast: List<CastMember>
)

@Serializable
data class CastMember(
    val name: String,
    val profile_path: String? = null
)