package com.example.mykultv2

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Data class to hold a favorite item with its timestamp
data class FavoriteItem(
    val item: Any, // Can be Movie, Book, or Music
    val timestamp: Long
)

object FavoritesManager {
    private val _favoriteMovies = mutableStateListOf<Movie>()
    private val _favoriteBooks = mutableStateListOf<Book>()
    private val _favoriteMusic = mutableStateListOf<Music>()
    private val _allFavorites = mutableStateListOf<FavoriteItem>()

    val favoriteMovies: SnapshotStateList<Movie> = _favoriteMovies
    val favoriteBooks: SnapshotStateList<Book> = _favoriteBooks
    val favoriteMusic: SnapshotStateList<Music> = _favoriteMusic
    val allFavorites: SnapshotStateList<FavoriteItem> = _allFavorites

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Load favorites from Firestore when the app starts, sorted by timestamp
    suspend fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Sort by timestamp
                    .get()
                    .await()
            }

            _favoriteMovies.clear()
            _favoriteBooks.clear()
            _favoriteMusic.clear()
            _allFavorites.clear()

            for (doc in snapshot.documents) {
                val type = doc.getString("type")
                val data = doc.getString("data") ?: continue
                val timestamp = doc.getLong("timestamp") ?: 0L // Get timestamp

                when (type) {
                    "movie" -> {
                        val movie = Json.decodeFromString<Movie>(data)
                        _favoriteMovies.add(movie)
                        _allFavorites.add(FavoriteItem(movie, timestamp))
                    }
                    "book" -> {
                        val book = Json.decodeFromString<Book>(data)
                        _favoriteBooks.add(book)
                        _allFavorites.add(FavoriteItem(book, timestamp))
                    }
                    "music" -> {
                        val music = Json.decodeFromString<Music>(data)
                        _favoriteMusic.add(music)
                        _allFavorites.add(FavoriteItem(music, timestamp))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error loading favorites: ${e.message}")
        }
    }

    // Toggle favorite for a movie
    suspend fun toggleFavoriteMovie(movie: Movie) {
        val userId = auth.currentUser?.uid ?: return
        val docId = "movie_${movie.id}"

        if (_favoriteMovies.contains(movie)) {
            // Remove from Firestore
            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(docId)
                    .delete()
                    .await()
            }
            _favoriteMovies.remove(movie)
            _allFavorites.removeAll { it.item == movie }
        } else {
            // Add to Firestore with timestamp
            val timestamp = System.currentTimeMillis()
            val data = hashMapOf(
                "type" to "movie",
                "data" to Json.encodeToString(movie),
                "timestamp" to timestamp
            )
            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()
            }
            _favoriteMovies.add(movie)
            _allFavorites.add(FavoriteItem(movie, timestamp))
            // Sort allFavorites by timestamp
            _allFavorites.sortByDescending { it.timestamp }
        }
    }

    // Toggle favorite for a book
    suspend fun toggleFavoriteBook(book: Book) {
        val userId = auth.currentUser?.uid ?: return
        val docId = "book_${book.id}"

        if (_favoriteBooks.contains(book)) {
            // Remove from Firestore
            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(docId)
                    .delete()
                    .await()
            }
            _favoriteBooks.remove(book)
            _allFavorites.removeAll { it.item == book }
        } else {
            // Add to Firestore with timestamp
            val timestamp = System.currentTimeMillis()
            val data = hashMapOf(
                "type" to "book",
                "data" to Json.encodeToString(book),
                "timestamp" to timestamp
            )
            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()
            }
            _favoriteBooks.add(book)
            _allFavorites.add(FavoriteItem(book, timestamp))
            // Sort allFavorites by timestamp
            _allFavorites.sortByDescending { it.timestamp }
        }
    }

    // Toggle favorite for music
    suspend fun toggleFavoriteMusic(music: Music) {
        val userId = auth.currentUser?.uid ?: return
        val docId = "music_${music.id}"

        if (_favoriteMusic.contains(music)) {
            // Remove from Firestore
            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(docId)
                    .delete()
                    .await()
            }
            _favoriteMusic.remove(music)
            _allFavorites.removeAll { it.item == music }
        } else {
            // Add to Firestore with timestamp
            val timestamp = System.currentTimeMillis()
            val data = hashMapOf(
                "type" to "music",
                "data" to Json.encodeToString(music),
                "timestamp" to timestamp
            )
            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()
            }
            _favoriteMusic.add(music)
            _allFavorites.add(FavoriteItem(music, timestamp))
            // Sort allFavorites by timestamp
            _allFavorites.sortByDescending { it.timestamp }
        }
    }

    fun isMovieFavorited(movie: Movie): Boolean {
        return _favoriteMovies.contains(movie)
    }

    fun isBookFavorited(book: Book): Boolean {
        return _favoriteBooks.contains(book)
    }

    fun isMusicFavorited(music: Music): Boolean {
        return _favoriteMusic.contains(music)
    }
    fun isTrackFavorited(track: Music): Boolean {
        return favoriteTracks[track.id] ?: false
    }suspend fun toggleFavoriteTrack(track: Music) {
        val currentStatus = isTrackFavorited(track)
        favoriteTracks[track.id] = !currentStatus
    }
    private val favoriteTracks = mutableStateMapOf<String, Boolean>()


    fun clearFavorites() {
        favoriteBooks.clear()
        favoriteMusic.clear()
        // Clear other favorite types if any
    }
}