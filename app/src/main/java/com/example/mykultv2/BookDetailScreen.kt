package com.mykult.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun BookDetailScreen(navController: NavHostController, bookId: String) {
    val json = Json { ignoreUnknownKeys = true }
    var bookDetails by remember { mutableStateOf<BookDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isFavorited by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        scope.launch {
            try {
                val detailsJson = withContext(Dispatchers.IO) {
                    URL("https://www.googleapis.com/books/v1/volumes/$bookId").readText()
                }
                bookDetails = json.decodeFromString<BookDetails>(detailsJson)

                bookDetails?.let { details ->
                    val book = Book(
                        id = details.id,
                        title = details.volumeInfo.title ?: "Unknown Title",
                        author = details.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                        imageUrl = details.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://") ?: "",
                        publishedDate = details.volumeInfo.publishedDate ?: "N/A"
                    )
                    RecentlyWatchedManager.addRecentlyWatchedBook(book)
                    isFavorited = FavoritesManager.isBookFavorited(book)
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Retour",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.popBackStack() },
                    tint = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Books Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Row {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favori",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                bookDetails?.let { details ->
                                    val book = Book(
                                        id = details.id,
                                        title = details.volumeInfo.title ?: "Unknown Title",
                                        author = details.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                                        imageUrl = details.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://") ?: "",
                                        publishedDate = details.volumeInfo.publishedDate ?: "N/A"
                                    )
                                    scope.launch {
                                        FavoritesManager.toggleFavoriteBook(book)
                                        isFavorited = FavoritesManager.isBookFavorited(book)
                                    }
                                }
                            },
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Partager",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                bookDetails?.let { details ->
                                    shareBook(context, details.volumeInfo.title ?: "Unknown Title", details.id)
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
                            text = error ?: "Erreur inconnue",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                bookDetails != null -> {
                    SubcomposeAsyncImage(
                        model = bookDetails?.volumeInfo?.imageLinks?.thumbnail?.replace("http://", "https://"),
                        contentDescription = "Couverture de ${bookDetails?.volumeInfo?.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            is AsyncImagePainter.State.Error -> Text("Erreur de chargement de l'image", color = MaterialTheme.colorScheme.onSecondary)
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                }
            }
        }

        if (bookDetails != null) {
            item {
                Text(
                    text = bookDetails?.volumeInfo?.title ?: "",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Text(
                    text = "Auteurs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookDetails?.volumeInfo?.authors?.joinToString(", ") ?: "N/A",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                Text(
                    text = "Profil des Auteurs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            bookDetails?.volumeInfo?.authors?.firstOrNull()?.let { author ->
                                navController.navigate("authorProfile/${Uri.encode(author)}")
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
                            text = "Nom: ${bookDetails?.volumeInfo?.authors?.joinToString(", ") ?: "N/A"}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sortie: ${bookDetails?.volumeInfo?.publishedDate ?: "N/A"}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Pages: ${bookDetails?.volumeInfo?.pageCount ?: "N/A"}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            item {
                Text(
                    text = "Éditeur",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookDetails?.volumeInfo?.publisher ?: "N/A",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                Text(
                    text = "Description",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Text(
                    text = bookDetails?.volumeInfo?.description ?: "Pas de description disponible",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                val readLink = bookDetails?.volumeInfo?.previewLink?.takeIf { it.isNotBlank() }
                    ?: bookDetails?.volumeInfo?.infoLink?.takeIf { it.isNotBlank() }
                    ?: "https://www.google.com/search?q=${Uri.encode(bookDetails?.volumeInfo?.title)}+book"

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(readLink))
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Read",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Read Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Helper function to share book details
private fun shareBook(context: Context, title: String, bookId: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out this book!")
        putExtra(Intent.EXTRA_TEXT, "I'm reading $title! Check it out: https://books.google.com/books?id=$bookId")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
}

@Serializable
data class BookDetails(
    val id: String,
    val volumeInfo: VolumeInfo
)

