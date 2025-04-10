package com.example.mykultv2

import android.annotation.SuppressLint
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder

@SuppressLint("UnrememberedMutableState")
@Composable
fun AuthorProfileScreen(navController: NavHostController, authorName: String) {
    val json = Json { ignoreUnknownKeys = true }
    var books by remember { mutableStateOf<List<BookDetails>>(emptyList()) }
    var totalItems by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pageSize = 10
    var currentPage by remember { mutableStateOf(0) }
    val totalPages by derivedStateOf { if (totalItems > 0) (totalItems + pageSize - 1) / pageSize else 1 }

    LaunchedEffect(authorName, currentPage) {
        scope.launch {
            try {
                isLoading = true
                val startIndex = currentPage * pageSize
                val encodedAuthor = URLEncoder.encode(authorName, "UTF-8")
                val booksJson = withContext(Dispatchers.IO) {
                    URL("https://www.googleapis.com/books/v1/volumes?q=inauthor:\"$encodedAuthor\"&startIndex=$startIndex&maxResults=$pageSize&langRestrict=en").readText()
                }
                val booksResponse = json.decodeFromString<BooksResponse>(booksJson)
                books = booksResponse.items ?: emptyList()
                totalItems = booksResponse.totalItems ?: 0
            } catch (e: Exception) {
                error = "Error loading books: ${e.message}"
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
                    text = "Author Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Name: $authorName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
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
                books.isEmpty() -> {
                    Text(
                        text = "No books found for this author.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                else -> {
                    Text(
                        text = "Books by Author",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (books.isNotEmpty()) {
            items(books) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("bookDetail/${book.id}")
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
                            model = book.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://"),
                            contentDescription = "Cover of ${book.volumeInfo.title}",
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
                                text = book.volumeInfo.title ?: "N/A",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Release: ${book.volumeInfo.publishedDate ?: "N/A"}",
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
                                        val bookToFavorite = Book(
                                            id = book.id,
                                            title = book.volumeInfo.title ?: "N/A",
                                            author = authorName,
                                            imageUrl = book.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://") ?: "",
                                            publishedDate = book.volumeInfo.publishedDate ?: "N/A"
                                        )
                                        FavoritesManager.toggleFavoriteBook(bookToFavorite)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (FavoritesManager.isBookFavorited(
                                        Book(
                                            id = book.id,
                                            title = book.volumeInfo.title ?: "N/A",
                                            author = authorName,
                                            imageUrl = book.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://") ?: "",
                                            publishedDate = book.volumeInfo.publishedDate ?: "N/A"
                                        )
                                    )) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (FavoritesManager.isBookFavorited(
                                        Book(
                                            id = book.id,
                                            title = book.volumeInfo.title ?: "N/A",
                                            author = authorName,
                                            imageUrl = book.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://") ?: "",
                                            publishedDate = book.volumeInfo.publishedDate ?: "N/A"
                                        )
                                    )) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Previous")
                    }

                    Text(
                        text = "Page ${currentPage + 1} of $totalPages",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Button(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next")
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

// Data Classes for Books Response (used in AuthorProfileScreen)
@Serializable
data class BooksResponse(
    val totalItems: Int? = null,
    val items: List<BookDetails>? = null
)