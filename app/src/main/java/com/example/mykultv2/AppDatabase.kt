package com.mykult.app

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase

// Room Entity
@Entity
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val password: String, // Note: Avoid storing plain passwords in production
    val roomId: String
)

// Room DAO
@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)
}

// Room Database
@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "culture_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}