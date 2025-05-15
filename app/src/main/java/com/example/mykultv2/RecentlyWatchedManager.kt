package com.mykult.app

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Data class to hold a recently watched item with its timestamp
data class RecentlyWatchedItem(
    val item: Any, // Can be Movie, Book, or Music
    val timestamp: Long
)

object RecentlyWatchedManager {
    private val _recentlyWatched = mutableStateListOf<RecentlyWatchedItem>()

    val recentlyWatched: SnapshotStateList<RecentlyWatchedItem> = _recentlyWatched

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Load recently watched items from Firestore, sorted by timestamp
    suspend fun loadRecentlyWatched(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("recently_watched")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            }

            _recentlyWatched.clear()

            for (doc in snapshot.documents) {
                val type = doc.getString("type")
                val data = doc.getString("data") ?: continue
                val timestamp = doc.getLong("timestamp") ?: 0L

                when (type) {
                    "movie" -> {
                        val movie = Json.decodeFromString<Movie>(data)
                        _recentlyWatched.add(RecentlyWatchedItem(movie, timestamp))
                    }
                    "book" -> {
                        val book = Json.decodeFromString<Book>(data)
                        _recentlyWatched.add(RecentlyWatchedItem(book, timestamp))
                    }
                    "music" -> {
                        val music = Json.decodeFromString<Music>(data)
                        _recentlyWatched.add(RecentlyWatchedItem(music, timestamp))
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add a movie to recently watched
    suspend fun addRecentlyWatchedMovie(movie: Movie) {
        val userId = auth.currentUser?.uid ?: return
        val docId = "movie_${movie.id}"

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
                .collection("recently_watched")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
        }

        // Remove any existing entry for this movie to avoid duplicates
        _recentlyWatched.removeAll { it.item == movie }
        // Add the new entry
        _recentlyWatched.add(RecentlyWatchedItem(movie, timestamp))
        // Sort by timestamp
        _recentlyWatched.sortByDescending { it.timestamp }
    }

    // Add a book to recently watched
    suspend fun addRecentlyWatchedBook(book: Book) {
        val userId = auth.currentUser?.uid ?: return
        val docId = "book_${book.id}"

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
                .collection("recently_watched")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
        }

        // Remove any existing entry for this book to avoid duplicates
        _recentlyWatched.removeAll { it.item == book }
        // Add the new entry
        _recentlyWatched.add(RecentlyWatchedItem(book, timestamp))
        // Sort by timestamp
        _recentlyWatched.sortByDescending { it.timestamp }
    }

    // Add music to recently watched
    suspend fun addRecentlyWatchedMusic(music: Music) {
        val userId = auth.currentUser?.uid ?: return
        val docId = "music_${music.id}"

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
                .collection("recently_watched")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
        }

        // Remove any existing entry for this music to avoid duplicates
        _recentlyWatched.removeAll { it.item == music }
        // Add the new entry
        _recentlyWatched.add(RecentlyWatchedItem(music, timestamp))
        // Sort by timestamp
        _recentlyWatched.sortByDescending { it.timestamp }
    }

    // Clear all recently watched items
    suspend fun clearRecentlyWatched() {
        val userId = auth.currentUser?.uid ?: return
        try {
            // Clear Firestore data
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("recently_watched")
                    .get()
                    .await()
            }

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            withContext(Dispatchers.IO) {
                batch.commit().await()
            }

            // Clear in-memory list
            _recentlyWatched.clear()
        } catch (e: Exception) {
            println("Error clearing recently watched: ${e.message}")
        }
    }
}