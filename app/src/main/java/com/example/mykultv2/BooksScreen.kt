package com.example.mykultv2

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import java.net.URL

@Composable
fun BooksScreen() {
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var booksError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var selectedGenre by remember { mutableStateOf<String?>(null) } // Null means "All Genres"
    val totalBooksToLoad = 100
    val booksPerPage = 20
    val maxPages = (totalBooksToLoad / booksPerPage) + if (totalBooksToLoad % booksPerPage > 0) 1 else 0

    val json = Json { ignoreUnknownKeys = true }
    val gridState = rememberLazyGridState()

    // Function to fetch books for a specific page and genre
    suspend fun fetchBooks(page: Int, genre: String? = null): List<Book> {
        return try {
            val genreQuery = when (genre) {
                "Fiction" -> "subject:fiction"
                "Non-fiction" -> "subject:nonfiction"
                "Mystère" -> "subject:mystery"
                "Romance" -> "subject:romance"
                else -> "bestsellers"
            }
            val startIndex = (page - 1) * booksPerPage
            val url = "https://www.googleapis.com/books/v1/volumes?q=$genreQuery&maxResults=$booksPerPage&startIndex=$startIndex&langRestrict=fr"
            Log.d("BooksScreen", "Fetching books from: $url") // Log the URL for debugging
            val booksJson = withContext(Dispatchers.IO) {
                URL(url).readText()
            }
            val jsonObject = Json.parseToJsonElement(booksJson).jsonObject
            val items = jsonObject["items"]?.jsonArray ?: return emptyList()

            items.mapNotNull { item ->
                val volumeInfo = item.jsonObject["volumeInfo"]?.jsonObject ?: return@mapNotNull null
                val title = volumeInfo["title"]?.toString()?.trim('"') ?: return@mapNotNull null // Skip if title is null
                val authors = volumeInfo["authors"]?.jsonArray?.joinToString(", ") { it.toString().trim('"') } ?: "Unknown Author"
                val imageLinks = volumeInfo["imageLinks"]?.jsonObject
                val thumbnail = imageLinks?.get("thumbnail")?.toString()?.trim('"')?.replace("http://", "https://") ?: ""
                val publishedDate = volumeInfo["publishedDate"]?.toString()?.trim('"') ?: "N/A"

                Book(
                    title = title,
                    author = authors,
                    imageUrl = thumbnail,
                    publishedDate = publishedDate
                )
            }
        } catch (e: Exception) {
            booksError = "Erreur lors du chargement des livres: ${e.message}"
            Log.e("BooksScreen", "Error fetching books: ${e.message}")
            emptyList()
        }
    }

    // Reset and fetch books when genre changes
    LaunchedEffect(selectedGenre) {
        currentPage = 1
        books = emptyList()
        isLoading = true
        val newBooks = fetchBooks(currentPage, selectedGenre)
        books = newBooks
        isLoading = false
    }

    // Load more books when the user scrolls to the bottom
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
                val totalItems = gridState.layoutInfo.totalItemsCount

                if (lastVisibleItem >= totalItems - 4 && !isLoading && currentPage < maxPages) {
                    isLoading = true
                    currentPage++
                    val newBooks = fetchBooks(currentPage, selectedGenre)
                    books = books + newBooks
                    isLoading = false
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Title
        Text(
            text = "Tous les Livres Populaires",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A00E0),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Category Selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All Genres" option
            item {
                FilterChip(
                    selected = selectedGenre == null,
                    onClick = { selectedGenre = null },
                    label = { Text("Tous les genres") },
                    modifier = Modifier.border(width = 0.dp, color = Color.Transparent),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4A00E0),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFE0E0E0),
                        labelColor = Color.Black
                    )
                )
            }

            // Genre chips
            val categories = listOf("Fiction", "Non-fiction", "Mystère", "Science-fiction", "Romance")
            items(categories) { category ->
                FilterChip(
                    selected = selectedGenre == category,
                    onClick = { selectedGenre = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4A00E0),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFE0E0E0),
                        labelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Books Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (booksError != null) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = booksError ?: "Erreur inconnue",
                            fontSize = 16.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(books) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SubcomposeAsyncImage(
                                model = book.imageUrl,
                                contentDescription = "Couverture de ${book.title}",
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            ) {
                                when (painter.state) {
                                    is AsyncImagePainter.State.Loading -> Box(
                                        modifier = Modifier.matchParentSize().background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }
                                    is AsyncImagePainter.State.Error -> Box(
                                        modifier = Modifier.matchParentSize().background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) { Text("Erreur", color = Color.White, fontSize = 12.sp) }
                                    else -> SubcomposeAsyncImageContent()
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = book.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sortie: ${book.publishedDate}",
                                fontSize = 12.sp,
                                color = Color(0xFF757575),
                                textAlign = TextAlign.Center
                            )
                        }

                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-12).dp, y = (-12).dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA500)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4A00E0),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFFE0E0E0),
            labelColor = Color.Black
        )
    )
}

