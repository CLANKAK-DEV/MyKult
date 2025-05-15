package com.mykult.app

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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





@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BooksScreen(navController: NavHostController) {
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var booksError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    val totalBooksToLoad = 100
    val booksPerPage = 20
    val maxPages = (totalBooksToLoad / booksPerPage) + if (totalBooksToLoad % booksPerPage > 0) 1 else 0

    val json = Json { ignoreUnknownKeys = true }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    suspend fun fetchBooks(genre: String? = null): List<Book> {
        return try {
            val genreQuery = when (genre) {
                "Fiction" -> "subject:fiction"
                "Non-fiction" -> "subject:nonfiction"
                "Mystery" -> "subject:mystery"
                "Science Fiction" -> "subject:scifi"
                "Romance" -> "subject:romance"
                else -> "bestsellers"
            }
            val randomStartIndex = Random.nextInt(0, 1000 - booksPerPage + 1)
            val url = "https://www.googleapis.com/books/v1/volumes?q=$genreQuery&maxResults=$booksPerPage&startIndex=$randomStartIndex&langRestrict=en"
            Log.d("BooksScreen", "Fetching random books from: $url")
            val booksJson = withContext(Dispatchers.IO) {
                URL(url).readText()
            }
            val jsonObject = Json.parseToJsonElement(booksJson).jsonObject
            val items = jsonObject["items"]?.jsonArray ?: return emptyList()

            items.mapNotNull { item ->
                val id = item.jsonObject["id"]?.toString()?.trim('"') ?: return@mapNotNull null
                val volumeInfo = item.jsonObject["volumeInfo"]?.jsonObject ?: return@mapNotNull null
                val title = volumeInfo["title"]?.toString()?.trim('"') ?: return@mapNotNull null
                val authors = volumeInfo["authors"]?.jsonArray?.joinToString(", ") { it.toString().trim('"') } ?: "Unknown Author"
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
            booksError = "Error fetching books: ${e.message}"
            Log.e("BooksScreen", "Error fetching books: ${e.message}")
            emptyList()
        }
    }

    suspend fun refreshBooks() {
        isRefreshing = true
        books = emptyList()
        val newBooks = fetchBooks(selectedGenre)
        books = newBooks
        isRefreshing = false
    }

    LaunchedEffect(selectedGenre) {
        currentPage = 1
        books = emptyList()
        isLoading = true
        val newBooks = fetchBooks(selectedGenre)
        books = newBooks
        isLoading = false
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()?.index ?: 0
                val totalItems = gridState.layoutInfo.totalItemsCount

                if (lastVisibleItem >= totalItems - 4 && !isLoading && currentPage < maxPages) {
                    isLoading = true
                    currentPage++
                    val newBooks = fetchBooks(selectedGenre)
                    books = books + newBooks
                    isLoading = false
                }
            }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { scope.launch { refreshBooks() } }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "All Popular Books",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedGenre == null,
                        onClick = { selectedGenre = null },
                        label = { Text("All Genres", fontSize = 12.sp) },
                        modifier = Modifier.border(width = 0.dp, color = MaterialTheme.colorScheme.background),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                val categories = listOf("Fiction", "Non-fiction", "Mystery", "Science Fiction", "Romance")
                items(categories) { category ->
                    FilterChip(
                        selected = selectedGenre == category,
                        onClick = { selectedGenre = category },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (booksError != null) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    text = booksError ?: "Unknown error",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                items(books) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { navController.navigate("bookDetail/${book.id}") },
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
                                    model = book.imageUrl,
                                    contentDescription = book.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.secondary),
                                    contentScale = ContentScale.Crop
                                ) {
                                    when (painter.state) {
                                        is AsyncImagePainter.State.Loading -> Box(
                                            modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.secondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        is AsyncImagePainter.State.Error -> Box(
                                            modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.secondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrokenImage,
                                                contentDescription = "Image error",
                                                tint = MaterialTheme.colorScheme.onSecondary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        else -> SubcomposeAsyncImageContent()
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)
                                        .clip(CircleShape)
                                        .clickable {
                                            scope.launch {
                                                FavoritesManager.toggleFavoriteBook(book)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (FavoritesManager.isBookFavorited(book)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (FavoritesManager.isBookFavorited(book)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                                    text = book.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Release: ${book.publishedDate.take(4)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.dp
                            )
                        }
                    }
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