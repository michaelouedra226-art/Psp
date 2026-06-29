package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String,
    val fileSize: Long,
    val format: String, // ISO, CSO, PBP
    val region: String = "US", // US, EU, JP
    val playTime: Long = 0, // In milliseconds
    val lastPlayed: Long = 0, // Timestamp
    val isFavorite: Boolean = false,
    val coverUrl: String? = null,
    val rating: Float = 4.5f,
    val genre: String = "Action"
)

@Entity(tableName = "save_states")
data class SaveState(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameId: Int,
    val slot: Int, // Slot 1 to 5 (0 is Autosave)
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val description: String = "State Save",
    val gameTitle: String = "PSP Game",
    val gameCoverUrl: String? = null
)
