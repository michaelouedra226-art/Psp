package com.example.data.repository

import com.example.data.database.GameDao
import com.example.data.model.Game
import com.example.data.model.SaveState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.File

class GameRepository(private val gameDao: GameDao) {

    val allGames: Flow<List<Game>> = gameDao.getAllGames()
    val favoriteGames: Flow<List<Game>> = gameDao.getFavoriteGames()
    val recentGames: Flow<List<Game>> = gameDao.getRecentGames()

    suspend fun getGameById(id: Int): Game? = gameDao.getGameById(id)

    suspend fun insertGame(game: Game): Long = gameDao.insertGame(game)

    suspend fun updateGame(game: Game) = gameDao.updateGame(game)

    suspend fun deleteGame(game: Game) = gameDao.deleteGame(game)

    // Save States
    val allSaveStates: Flow<List<SaveState>> = gameDao.getAllSaveStates()

    fun getSaveStatesForGame(gameId: Int): Flow<List<SaveState>> = gameDao.getSaveStatesForGame(gameId)

    suspend fun getSaveStateBySlot(gameId: Int, slot: Int): SaveState? = gameDao.getSaveStateBySlot(gameId, slot)

    suspend fun insertSaveState(saveState: SaveState): Long = gameDao.insertSaveState(saveState)

    suspend fun deleteSaveState(saveState: SaveState) = gameDao.deleteSaveState(saveState)

    /**
     * Scans a local directory recursively for PSP games (ISO, CSO, PBP).
     */
    suspend fun scanDirectory(directoryPath: String): List<Game> {
        val rootDir = File(directoryPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()

        val foundGames = mutableListOf<Game>()
        scanDirRecursive(rootDir, foundGames, 0)
        return foundGames
    }

    private suspend fun scanDirRecursive(dir: File, foundGames: MutableList<Game>, depth: Int) {
        if (depth > 4) return // Avoid scanning too deep
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirRecursive(file, foundGames, depth + 1)
            } else {
                val extension = file.extension.uppercase()
                if (extension == "ISO" || extension == "CSO" || extension == "PBP") {
                    val existing = gameDao.getGameByPath(file.absolutePath)
                    if (existing == null) {
                        val format = extension
                        val title = file.nameWithoutExtension.replace("_", " ").replace("-", " ")
                        val size = file.length()
                        val region = when {
                            title.contains("US") || title.contains("USA") -> "US"
                            title.contains("JP") || title.contains("JAP") || title.contains("JPN") -> "JP"
                            else -> "EU"
                        }
                        val coverUrl = getCoverArtForTitle(title)
                        val newGame = Game(
                            title = title,
                            filePath = file.absolutePath,
                            fileSize = size,
                            format = format,
                            region = region,
                            rating = (42..50).random().toFloat() / 10f,
                            genre = detectGenre(title),
                            coverUrl = coverUrl
                        )
                        gameDao.insertGame(newGame)
                        foundGames.add(newGame)
                    }
                }
            }
        }
    }

    private fun getCoverArtForTitle(title: String): String {
        val t = title.uppercase()
        return when {
            t.contains("WIPEOUT") -> "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=300&auto=format&fit=crop"
            t.contains("RIDGE") || t.contains("RACE") -> "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=300&auto=format&fit=crop"
            t.contains("GOD OF WAR") || t.contains("SPARTA") || t.contains("OLYMPUS") -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300&auto=format&fit=crop"
            t.contains("MONSTER HUNTER") -> "https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=300&auto=format&fit=crop"
            t.contains("FLAPPY") -> "https://images.unsplash.com/photo-1516116211223-4c359a36be48?w=300&auto=format&fit=crop"
            t.contains("CUBE") || t.contains("SANDBOX") -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&auto=format&fit=crop"
            t.contains("SOLDIER") || t.contains("SHOOT") -> "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop"
            else -> {
                // Return a stylish cyber-retro gradient representation
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&auto=format&fit=crop&q=80"
            }
        }
    }

    /**
     * Populates high-quality, pre-configured homebrew and demo games on first run
     * so that the frontend has instantly playable titles with gorgeous covers.
     */
    suspend fun prepopulateDemoGamesIfNeeded() {
        val currentGames = allGames.firstOrNull()
        if (currentGames.isNullOrEmpty()) {
            val demos = listOf(
                Game(
                    title = "Wipeout Pure (Demo)",
                    filePath = "virtual://games/wipeout_pure_demo.iso",
                    fileSize = 145228100, // ~145 MB
                    format = "ISO",
                    region = "US",
                    playTime = 0,
                    lastPlayed = 0,
                    isFavorite = true,
                    rating = 4.8f,
                    genre = "Racing",
                    coverUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=300&auto=format&fit=crop"
                ),
                Game(
                    title = "Ridge Racer (Demo Version)",
                    filePath = "virtual://games/ridge_racer.cso",
                    fileSize = 98412032, // ~98 MB
                    format = "CSO",
                    region = "EU",
                    playTime = 0,
                    lastPlayed = 0,
                    isFavorite = false,
                    rating = 4.6f,
                    genre = "Racing",
                    coverUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=300&auto=format&fit=crop"
                ),
                Game(
                    title = "Cube 3D Physics Sandbox (Homebrew)",
                    filePath = "virtual://games/cube_3d_sandbox.pbp",
                    fileSize = 12582912, // ~12 MB
                    format = "PBP",
                    region = "US",
                    playTime = 0,
                    lastPlayed = 0,
                    isFavorite = true,
                    rating = 4.2f,
                    genre = "Simulation",
                    coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&auto=format&fit=crop"
                ),
                Game(
                    title = "Flappy Bird PSP (Homebrew Classic)",
                    filePath = "virtual://games/flappy_bird_psp.pbp",
                    fileSize = 3145728, // ~3 MB
                    format = "PBP",
                    region = "EU",
                    playTime = 0,
                    lastPlayed = 0,
                    isFavorite = false,
                    rating = 4.1f,
                    genre = "Arcade",
                    coverUrl = "https://images.unsplash.com/photo-1516116211223-4c359a36be48?w=300&auto=format&fit=crop"
                ),
                Game(
                    title = "Star Soldier (Special Demo)",
                    filePath = "virtual://games/star_soldier_demo.iso",
                    fileSize = 58920192, // ~58 MB
                    format = "ISO",
                    region = "JP",
                    playTime = 0,
                    lastPlayed = 0,
                    isFavorite = false,
                    rating = 4.4f,
                    genre = "Shooter",
                    coverUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop"
                )
            )
            for (demo in demos) {
                gameDao.insertGame(demo)
            }
        }
    }

    private fun detectGenre(title: String): String {
        val t = title.uppercase()
        return when {
            t.contains("RACE") || t.contains("SPEED") || t.contains("WIPEOUT") || t.contains("RIDGE") -> "Racing"
            t.contains("WAR") || t.contains("FIGHT") || t.contains("TEKKEN") || t.contains("STREET") -> "Fighting"
            t.contains("SHOOT") || t.contains("SOLDIER") || t.contains("STRIKE") -> "Shooter"
            t.contains("CUBE") || t.contains("SANDBOX") -> "Simulation"
            t.contains("BIRD") || t.contains("MARIO") || t.contains("PLATFORM") -> "Arcade"
            else -> "Action"
        }
    }
}
