package com.example.data.database

import androidx.room.*
import com.example.data.model.Game
import com.example.data.model.SaveState
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY title ASC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE isFavorite = 1 ORDER BY lastPlayed DESC")
    fun getFavoriteGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE lastPlayed > 0 ORDER BY lastPlayed DESC")
    fun getRecentGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Int): Game?

    @Query("SELECT * FROM games WHERE filePath = :path")
    suspend fun getGameByPath(path: String): Game?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    @Delete
    suspend fun deleteGame(game: Game)

    // Save States
    @Query("SELECT * FROM save_states ORDER BY timestamp DESC")
    fun getAllSaveStates(): Flow<List<SaveState>>

    @Query("SELECT * FROM save_states WHERE gameId = :gameId ORDER BY slot ASC")
    fun getSaveStatesForGame(gameId: Int): Flow<List<SaveState>>

    @Query("SELECT * FROM save_states WHERE gameId = :gameId AND slot = :slot")
    suspend fun getSaveStateBySlot(gameId: Int, slot: Int): SaveState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaveState(saveState: SaveState): Long

    @Delete
    suspend fun deleteSaveState(saveState: SaveState)
}
