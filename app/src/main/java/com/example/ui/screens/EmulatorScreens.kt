package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Game
import com.example.data.model.SaveState
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// --- MAIN EMULATOR SHELL ---

@Composable
fun EmulatorAppShell(viewModel: EmulatorViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }

    // Support Back Button
    BackHandler(enabled = true) {
        if (currentScreen != Screen.Library) {
            if (currentScreen == Screen.Play) {
                viewModel.exitGame()
            } else {
                val popped = viewModel.navigateBack()
                if (!popped) {
                    viewModel.navigateTo(Screen.Library)
                }
            }
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                android.widget.Toast.makeText(context, "Appuyez une seconde fois pour quitter.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isConnected by viewModel.isConnected.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAbyss)
    ) {
        AnimatedVisibility(
            visible = !isConnected,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEF4444))
                    .statusBarsPadding()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Mode Hors ligne actif - Émulation locale",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    Screen.Library -> LibraryScreen(viewModel)
                    Screen.SaveStates -> SaveStatesManagerScreen(viewModel)
                    Screen.Play -> PlayScreen(viewModel)
                    Screen.Settings -> SettingsScreen(viewModel)
                    Screen.CustomizeControls -> CustomizeControlsScreen(viewModel)
                }
            }
        }

        // Show navigation bar only for main dashboard screens (not while active in game)
        if (currentScreen != Screen.Play) {
            NavigationBar(
                containerColor = SurfaceMetal,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_navigation_bar")
                    .border(BorderStroke(1.dp, BorderMetal))
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.Library,
                    onClick = { viewModel.navigateTo(Screen.Library) },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Console") },
                    label = { Text("Console", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNeon,
                        selectedTextColor = PrimaryNeon,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = PrimaryNeon.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.SaveStates,
                    onClick = { viewModel.navigateTo(Screen.SaveStates) },
                    icon = { Icon(Icons.Default.Save, contentDescription = "Sauvegardes") },
                    label = { Text("Sauvegardes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNeon,
                        selectedTextColor = PrimaryNeon,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = PrimaryNeon.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.CustomizeControls,
                    onClick = { viewModel.navigateTo(Screen.CustomizeControls) },
                    icon = { Icon(Icons.Default.Gamepad, contentDescription = "Touches") },
                    label = { Text("Touches", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNeon,
                        selectedTextColor = PrimaryNeon,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = PrimaryNeon.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.Settings,
                    onClick = { viewModel.navigateTo(Screen.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Paramètres") },
                    label = { Text("Paramètres", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryNeon,
                        selectedTextColor = PrimaryNeon,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = PrimaryNeon.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}

// --- UTILS: TACTILE VIBRATION ---
fun triggerVibration(context: Context, strength: Float) {
    if (strength <= 0f) return
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val duration = (40 * strength).toLong()
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(duration, (255 * strength).toInt().coerceIn(1, 255)))
    } catch (e: Exception) {
        // No vibration permission or hardware
    }
}

// --- SCREEN 1: LIBRARY SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: EmulatorViewModel) {
    val allGames by viewModel.allGames.collectAsState()
    val favoriteGames by viewModel.favoriteGames.collectAsState()
    val recentGames by viewModel.recentGames.collectAsState()
    
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val profile by viewModel.performanceProfile.collectAsState()

    var selectedGameDetails by remember { mutableStateOf<Game?>(null) }
    var showScanFolderDialog by remember { mutableStateOf(false) }
    var customScanPath by remember { mutableStateOf("/storage/emulated/0/PSP/GAMES") }

    val filteredGames = remember(allGames, favoriteGames, recentGames, currentTab, searchQuery, sortType) {
        val baseList = when (currentTab) {
            LibraryTab.All -> allGames
            LibraryTab.Favorites -> favoriteGames
            LibraryTab.Recents -> recentGames
            LibraryTab.Stats -> allGames
            LibraryTab.Features -> emptyList()
        }
        val searched = if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        
        when (sortType) {
            SortType.Title -> searched.sortedBy { it.title }
            SortType.Size -> searched.sortedByDescending { it.fileSize }
            SortType.PlayTime -> searched.sortedByDescending { it.playTime }
            SortType.LastPlayed -> searched.sortedByDescending { it.lastPlayed }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PRO EMULATOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNeon,
                            letterSpacing = 1.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Nova",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Text(
                                text = "PSP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = PrimaryNeon
                            )
                        }
                    }
                },
                actions = {
                    val context = LocalContext.current
                    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            viewModel.importGameFromUri(context, uri)
                        }
                    }

                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.testTag("import_game_top_button")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Importer un Jeu", tint = PrimaryNeon)
                    }
                    IconButton(
                        onClick = { showScanFolderDialog = true },
                        modifier = Modifier.testTag("scan_folder_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Scanner", tint = PrimaryNeon)
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Paramètres", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundAbyss,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            if (currentTab == LibraryTab.All) {
                val context = LocalContext.current
                val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        viewModel.importGameFromUri(context, uri)
                    }
                }
                
                ExtendedFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = PrimaryNeon,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Importer .ISO/.CSO", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_import_game")
                )
            }
        },
        containerColor = BackgroundAbyss
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- PREMIUM PROFILE SELECTOR (SOPHISTICATED DARK HTML) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val profiles = listOf(
                        PerformanceProfile.Ultra to "ULTRA",
                        PerformanceProfile.Performance to "PERF",
                        PerformanceProfile.Balanced to "BALANCED",
                        PerformanceProfile.Economy to "ECO"
                    )
                    profiles.forEach { (p, label) ->
                        val active = profile == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) PrimaryNeon else Color.Transparent)
                                .clickable { viewModel.updatePerformanceProfile(p) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else TextSecondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Active profile state details (cpu, temp, battery)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fréq: ${profile.cpuFreq}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Temp: ${profile.temp}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Batt: ${profile.batteryUsage}",
                        fontSize = 11.sp,
                        color = AccentNeon,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- SEARCH BAR & TOGGLE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Rechercher un jeu PSP...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceMetal,
                        unfocusedContainerColor = SurfaceMetal,
                        focusedBorderColor = PrimaryNeon,
                        unfocusedBorderColor = BorderMetal,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("search_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.toggleViewMode() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceMetal)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = PrimaryNeon
                    )
                }
            }

            // --- CATEGORY TABS ---
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = PrimaryNeon,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                        color = PrimaryNeon,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                LibraryTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    Tab(
                        selected = selected,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    LibraryTab.All -> "Bibliothèque"
                                    LibraryTab.Favorites -> "Favoris"
                                    LibraryTab.Recents -> "Récents"
                                    LibraryTab.Stats -> "Statistiques"
                                    LibraryTab.Features -> "Fonctions PPSSPP"
                                },
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) PrimaryNeon else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- SORT CHIPS ---
            if (currentTab != LibraryTab.Stats && currentTab != LibraryTab.Features) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortType.values().forEach { sort ->
                        val active = sortType == sort
                        SuggestionChip(
                            onClick = { viewModel.setSortType(sort) },
                            label = {
                                Text(
                                    text = when (sort) {
                                        SortType.Title -> "Nom"
                                        SortType.Size -> "Taille"
                                        SortType.PlayTime -> "Temps de Jeu"
                                        SortType.LastPlayed -> "Dernier Lancé"
                                    },
                                    fontSize = 11.sp,
                                    color = if (active) Color.White else TextSecondary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (active) PrimaryNeon else SurfaceMetal,
                                labelColor = if (active) Color.White else TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (active) PrimaryNeon else BorderMetal)
                        )
                    }
                }
            }

            // --- LIBRARY CONTENT ---
            Box(modifier = Modifier.weight(1f)) {
                if (currentTab == LibraryTab.Stats) {
                    StatsDashboard(allGames)
                } else if (currentTab == LibraryTab.Features) {
                    FeaturesExplorer(viewModel)
                } else if (filteredGames.isEmpty()) {
                    EmptyState(tab = currentTab)
                } else {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGames, key = { it.id }) { game ->
                                GameGridCard(
                                    game = game,
                                    onPlayClick = { viewModel.selectGameAndPlay(game) },
                                    onLongClick = { selectedGameDetails = game },
                                    onFavToggle = { viewModel.toggleFavorite(game) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredGames, key = { it.id }) { game ->
                                GameListRow(
                                    game = game,
                                    onPlayClick = { viewModel.selectGameAndPlay(game) },
                                    onLongClick = { selectedGameDetails = game },
                                    onFavToggle = { viewModel.toggleFavorite(game) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- GAME DETAILS DIALOG ---
    selectedGameDetails?.let { game ->
        GameDetailsDialog(
            game = game,
            onDismiss = { selectedGameDetails = null },
            onPlay = {
                selectedGameDetails = null
                viewModel.selectGameAndPlay(game)
            },
            onToggleFavorite = { viewModel.toggleFavorite(game) },
            onDelete = {
                viewModel.deleteGame(game)
                selectedGameDetails = null
            }
        )
    }

    // --- SCAN FOLDER DIALOG ---
    if (showScanFolderDialog) {
        AlertDialog(
            onDismissRequest = { showScanFolderDialog = false },
            title = { Text("Analyse du stockage", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Saisissez ou sélectionnez le chemin d'accès local contenant vos fichiers ISO, CSO ou PBP.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = customScanPath,
                        onValueChange = { customScanPath = it },
                        label = { Text("Chemin du dossier PSP") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceSteel,
                            unfocusedContainerColor = SurfaceSteel
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadCustomFolder(customScanPath)
                        showScanFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Lancer l'analyse", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScanFolderDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = SurfaceMetal
        )
    }
}

// --- SUB-COMPONENT: GAME GRID CARD ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameGridCard(
    game: Game,
    onPlayClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .combinedClickable(
                onClick = onPlayClick,
                onLongClick = onLongClick
            )
            .testTag("game_card_${game.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
        border = BorderStroke(1.dp, BorderMetal)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // COVER IMAGE OR NEON GRAPHICS FALLBACK
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(SurfaceSteel, SurfaceMetal)
                        )
                    )
            ) {
                if (game.coverUrl != null) {
                    val painter = coil.compose.rememberAsyncImagePainter(game.coverUrl)
                    Image(
                        painter = painter,
                        contentDescription = "Game Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                // Drawing PSP looking cyber shapes for high quality aesthetic
                                val p = Path()
                                p.moveTo(0f, 0f)
                                p.lineTo(size.width, size.height * 0.7f)
                                p.lineTo(size.width, size.height)
                                p.lineTo(0f, size.height * 0.4f)
                                p.close()
                                drawPath(
                                    p, Brush.radialGradient(
                                        listOf(PrimaryNeon.copy(alpha = 0.2f), Color.Transparent),
                                        center = Offset(size.width / 2, size.height / 2)
                                    )
                                )
                                // Dynamic cross indicators
                                drawCircle(
                                    color = PrimaryNeon.copy(alpha = 0.4f),
                                    radius = 6.dp.toPx(),
                                    center = Offset(size.width * 0.25f, size.height * 0.4f),
                                    style = Stroke(2.dp.toPx())
                                )
                                drawLine(
                                    color = ButtonRed.copy(alpha = 0.4f),
                                    start = Offset(size.width * 0.75f - 8.dp.toPx(), size.height * 0.3f),
                                    end = Offset(size.width * 0.75f + 8.dp.toPx(), size.height * 0.3f),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawLine(
                                    color = ButtonRed.copy(alpha = 0.4f),
                                    start = Offset(size.width * 0.75f, size.height * 0.3f - 8.dp.toPx()),
                                    end = Offset(size.width * 0.75f, size.height * 0.3f + 8.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                    )
                    // Large styled title initials
                    Text(
                        text = game.title.take(2).uppercase(),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryNeon.copy(alpha = 0.15f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Region badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(game.region, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                // Format badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryNeon.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(game.format, fontSize = 9.sp, color = PrimaryNeon, fontWeight = FontWeight.Black)
                }
            }

            // FAVORITE ICON OVERLAY
            IconButton(
                onClick = onFavToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 90.dp, x = (-4).dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favori",
                    tint = if (game.isFavorite) FavoriteGold else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // METADATA TEXTS AT BOTTOM
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = game.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${"%.1f".format(game.fileSize.toDouble() / 1024 / 1024)} Mo",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = FavoriteGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "%.1f".format(game.rating),
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (game.playTime > 0) "Joué: ${formatPlaytime(game.playTime)}" else "Jamais lancé",
                    fontSize = 10.sp,
                    color = if (game.playTime > 0) AccentNeon else TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// --- SUB-COMPONENT: GAME LIST ROW ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameListRow(
    game: Game,
    onPlayClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .combinedClickable(
                onClick = onPlayClick,
                onLongClick = onLongClick
            )
            .testTag("game_row_${game.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
        border = BorderStroke(1.dp, BorderMetal)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cyber visual game cover thumbnail
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(SurfaceSteel)
                    .drawBehind {
                        drawCircle(
                            color = PrimaryNeon.copy(alpha = 0.2f),
                            radius = size.minDimension / 3f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    game.title.take(1).uppercase(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryNeon,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Metadata
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryNeon.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(game.format, fontSize = 8.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${"%.1f".format(game.fileSize.toDouble() / 1024 / 1024)} Mo",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = game.genre,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (game.playTime > 0) "Temps de jeu: ${formatPlaytime(game.playTime)}" else "Jamais lancé",
                    fontSize = 10.sp,
                    color = if (game.playTime > 0) AccentNeon else TextSecondary
                )
            }

            // Favorite button
            IconButton(onClick = onFavToggle) {
                Icon(
                    imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favori",
                    tint = if (game.isFavorite) FavoriteGold else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- STATS DASHBOARD TAB ---
@Composable
fun StatsDashboard(games: List<Game>) {
    val totalGames = games.size
    val totalPlayTime = games.sumOf { it.playTime }
    val averageRating = if (games.isNotEmpty()) games.map { it.rating }.average() else 4.5
    val favoriteCount = games.count { it.isFavorite }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Analyses d'Emulation Globales", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryNeon)
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsCard(
                    title = "Jeux Détectés",
                    value = "$totalGames",
                    subtitle = "Format ISO/CSO/PBP",
                    icon = Icons.Default.Games,
                    color = PrimaryNeon,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Favoris",
                    value = "$favoriteCount",
                    subtitle = "Accès rapide",
                    icon = Icons.Default.Star,
                    color = FavoriteGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsCard(
                    title = "Temps de Jeu Total",
                    value = formatPlaytime(totalPlayTime),
                    subtitle = "Emulation Active",
                    icon = Icons.Default.HourglassEmpty,
                    color = AccentNeon,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Évaluation Moyenne",
                    value = "%.1f / 5.0".format(averageRating),
                    subtitle = "Qualité ROMs",
                    icon = Icons.Default.ThumbUp,
                    color = ButtonBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
                border = BorderStroke(1.dp, BorderMetal)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Matériel & Noyau d'Emulation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    StatsRow(label = "Pilote Graphique Actif", value = "Vulkan API (Vulkan 1.3)", color = PrimaryNeon)
                    StatsRow(label = "Optimisation JIT", value = "Activé (Dynarec ARM64)", color = AccentNeon)
                    StatsRow(label = "Latence Audio Estimée", value = "12 ms (Très faible)", color = AccentNeon)
                    StatsRow(label = "Structure du Système", value = "Noyau PPSSPP v1.17 Compliant", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
        border = BorderStroke(1.dp, BorderMetal)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = TextSecondary)
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun StatsRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FeaturesExplorer(viewModel: EmulatorViewModel) {
    val context = LocalContext.current
    val graphics by viewModel.graphicsSettings.collectAsState()
    val audio by viewModel.audioSettings.collectAsState()
    val profile by viewModel.performanceProfile.collectAsState()
    var featureQuery by remember { mutableStateOf("") }
    
    val featuresList = remember {
        listOf(
            PspFeature(
                id = "vulkan",
                title = "Moteur de Rendu Vulkan 1.3",
                category = "Graphismes",
                description = "Pilote d'affichage moderne à haute efficacité. Vulkan réduit considérablement la surcharge du processeur (CPU overhead), ce qui augmente la stabilité du taux de rafraîchissement (FPS) et diminue fortement la chauffe thermique de l'appareil.",
                impact = "+25% à +50% FPS stables sur les processeurs modernes.",
                technicalDetails = "Supporte la mise en mémoire tampon directe des commandes GPU et l'optimisation des descripteurs de shader.",
                optimalValue = "Activé (Vulkan 1.3)",
                onApply = { vm ->
                    vm.updateGraphicsSettings { old -> old.copy(renderer = RendererType.Vulkan) }
                }
            ),
            PspFeature(
                id = "frameskip",
                title = "Saut de Trames (Frame Skipping)",
                category = "Graphismes",
                description = "Permet de sauter automatiquement ou manuellement le rendu de certaines trames d'animation lorsque le GPU est surchargé. Très utile pour conserver une vitesse de jeu à 100% sur des processeurs d'entrée de gamme.",
                impact = "Double la vitesse globale de l'émulation sur les appareils plus anciens.",
                technicalDetails = "L'algorithme analyse l'écart entre le temps d'horloge du CPU virtuel de la PSP et le CPU réel de l'hôte.",
                optimalValue = "Saut d'images: Désactivé (0x) sur les hauts de gamme, Automatique sur les entrée de gamme.",
                onApply = { vm ->
                    vm.updateGraphicsSettings { old -> old.copy(frameSkip = 0) }
                }
            ),
            PspFeature(
                id = "jit",
                title = "Compilateur Dynarec JIT",
                category = "Système",
                description = "Traduit dynamiquement en temps réel le code d'origine de la PSP (MIPS) en instructions machine natives d'Android (ARM64). Évite l'interprétation logicielle ligne par ligne.",
                impact = "Augmentation phénoménale des performances CPU par un facteur de 10x.",
                technicalDetails = "Gère les instructions complexes vectorielles (VFPU) de la PSP pour des calculs 3D ultra-rapides.",
                optimalValue = "Toujours Activé pour le profil Ultra Boost",
                onApply = { vm ->
                    vm.updatePerformanceProfile(PerformanceProfile.Ultra)
                }
            ),
            PspFeature(
                id = "texturescale",
                title = "Mise à l'échelle xBRZ & Bicubique",
                category = "Graphismes",
                description = "Algorithme intelligent qui extrapole et recrée les détails des textures 2D basse résolution d'origine de la PSP pour les rendre d'une netteté époustouflante sur les écrans modernes à haute densité.",
                impact = "Supprime le flou pixelisé rétro sur les éléments d'interface et d'environnement.",
                technicalDetails = "Utilise l'interpolation par analyse de courbes géométriques pour éviter le flou de floutage bilinéaire classique.",
                optimalValue = "Échelle: 3x PSP",
                onApply = { vm ->
                    vm.updateGraphicsSettings { old -> old.copy(resolutionScale = 3) }
                }
            ),
            PspFeature(
                id = "adhoc",
                title = "Réseau Multijoueur Pro AdHoc",
                category = "Réseau",
                description = "Simule de façon transparente le matériel Wi-Fi d'origine de la PSP via le protocole TCP/IP d'Android. Permet d'ouvrir des salons de jeu multijoueurs locaux ou mondiaux.",
                impact = "Jeu coopératif et versus fonctionnel à 100% sur les titres compatibles.",
                technicalDetails = "Établit un canal d'émulation de carte réseau WLAN virtuelle reliée à l'adresse de diffusion locale ou distante.",
                optimalValue = "Faible Latence Audio & Synchronisation Réseau active.",
                onApply = { vm ->
                    vm.updateAudioSettings { old -> old.copy(lowLatency = true, audioSync = true) }
                }
            ),
            PspFeature(
                id = "audio_lat",
                title = "Moteur Audio à Ultra Faible Latence",
                category = "Audio",
                description = "Technologie de synchronisation du flux sonore qui réduit l'écart entre l'action affichée à l'écran et la diffusion du son dans les écouteurs.",
                impact = "Élimine les décalages de son et les bruits parasites (crépitements).",
                technicalDetails = "Utilise des tailles de tampons audio dynamiques (AudioTrack asynchrone) adaptées à la fréquence d'échantillonnage de l'appareil.",
                optimalValue = "Latence Faible: Activée",
                onApply = { vm ->
                    vm.updateAudioSettings { old -> old.copy(lowLatency = true, volume = 0.9f) }
                }
            ),
            PspFeature(
                id = "cheat_eng",
                title = "Moteur de Triche CWCheats Actif",
                category = "Système",
                description = "Module d'écriture directe en mémoire virtuelle permettant de modifier les valeurs physiques des jeux (santé infinie, munitions illimitées, caméras débloquées ou patchs de taux d'image à 60fps).",
                impact = "Personnalisation absolue du comportement des jeux d'origine.",
                technicalDetails = "Injecte des codes hexadécimaux à des adresses mémoires spécifiques décalées de l'espace d'adressage virtuel de la PSP.",
                optimalValue = "Prêt à l'injection",
                onApply = { vm ->
                    android.widget.Toast.makeText(context, "Moteur CWCheats synchronisé ! Prêt pour vos fichiers .ini", android.widget.Toast.LENGTH_LONG).show()
                }
            ),
            PspFeature(
                id = "aniso_filt",
                title = "Filtrage Anisotrope 16x",
                category = "Graphismes",
                description = "Technique de filtrage de texture qui élimine le flou des textures observées sous des angles de caméra très obliques (comme la surface d'une route en perspective de course).",
                impact = "Arrière-plans et sols d'une netteté cristalline s'étendant jusqu'à l'horizon.",
                technicalDetails = "Calcule des échantillons de textures mip-map directionnels basés sur l'angle de projection de la caméra 3D.",
                optimalValue = "Filtrage: 16x",
                onApply = { vm ->
                    vm.updateGraphicsSettings { old -> old.copy(anisotropicFiltering = 16, antiAliasing = true) }
                }
            )
        )
    }

    val filteredFeatures = remember(featureQuery) {
        if (featureQuery.isBlank()) {
            featuresList
        } else {
            featuresList.filter {
                it.title.contains(featureQuery, ignoreCase = true) ||
                it.category.contains(featureQuery, ignoreCase = true) ||
                it.description.contains(featureQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Explorateur de Fonctionnalités",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = PrimaryNeon,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Recherchez et optimisez instantanément les fonctionnalités les plus minutieuses du noyau de l'émulateur pour vos jeux.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = featureQuery,
            onValueChange = { featureQuery = it },
            placeholder = { Text("Rechercher une fonction (ex: Vulkan, JIT...)", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryNeon) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceMetal,
                unfocusedContainerColor = SurfaceMetal,
                focusedBorderColor = PrimaryNeon,
                unfocusedBorderColor = BorderMetal,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        if (filteredFeatures.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune fonctionnalité trouvée pour '$featureQuery'", color = TextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredFeatures, key = { it.id }) { feat ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
                        border = BorderStroke(1.dp, BorderMetal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = feat.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryNeon.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = feat.category.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = feat.description,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceSteel.copy(alpha = 0.5f))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = AccentNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Noyau technique: ${feat.technicalDetails}",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = AccentNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Impact Performance: ${feat.impact}",
                                        fontSize = 10.sp,
                                        color = AccentNeon,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("RÉGLAGE OPTIMAL", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    Text(feat.optimalValue, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        feat.onApply(viewModel)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Réglage optimisé appliqué : ${feat.title} !",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Activer & Optimiser", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PspFeature(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val impact: String,
    val technicalDetails: String,
    val optimalValue: String,
    val onApply: (EmulatorViewModel) -> Unit
)

// --- EMPTY STATE ---
@Composable
fun EmptyState(tab: LibraryTab) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (tab) {
                LibraryTab.Favorites -> Icons.Default.StarOutline
                LibraryTab.Recents -> Icons.Default.History
                else -> Icons.Default.FolderOff
            },
            contentDescription = "Empty",
            tint = BorderMetal,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (tab) {
                LibraryTab.Favorites -> "Aucun jeu favori"
                LibraryTab.Recents -> "Aucun jeu récemment lancé"
                else -> "Aucun jeu PSP trouvé"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (tab) {
                LibraryTab.Favorites -> "Marquez vos titres préférés d'une étoile pour les regrouper ici."
                LibraryTab.Recents -> "Les jeux auxquels vous avez joué s'afficheront dans cet historique."
                else -> "Placez des fichiers .ISO, .CSO ou .PBP dans la mémoire de votre appareil, puis utilisez l'icône de dossier pour lancer une recherche."
            },
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}

// --- GAME DETAILS DIALOG ---
@Composable
fun GameDetailsDialog(
    game: Game,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
            border = BorderStroke(1.dp, BorderMetal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Détails du Jeu",
                    color = PrimaryNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Cover preview inside Dialog
                if (game.coverUrl != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(bottom = 12.dp)
                            .border(1.dp, BorderMetal, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val painter = coil.compose.rememberAsyncImagePainter(game.coverUrl)
                        Image(
                            painter = painter,
                            contentDescription = "Game Cover Header",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                // Title
                Text(game.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = BorderMetal, modifier = Modifier.padding(vertical = 4.dp))

                val lastPlayedFormatted = remember(game.lastPlayed) {
                    if (game.lastPlayed <= 0L) "Jamais"
                    else {
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(game.lastPlayed))
                    }
                }

                // Metadata Rows
                DetailRow("Chemin", game.filePath)
                DetailRow("Format de l'image", game.format)
                DetailRow("Taille du fichier", "${"%.2f".format(game.fileSize.toDouble() / 1024 / 1024)} Mo")
                DetailRow("Région", when(game.region) { "US" -> "États-Unis (NTSC-U)" "JP" -> "Japon (NTSC-J)" else -> "Europe (PAL)" })
                DetailRow("Temps de jeu", if (game.playTime > 0) formatPlaytime(game.playTime) else "0 minute")
                DetailRow("Dernière ouverture", lastPlayedFormatted)
                DetailRow("Genre", game.genre)

                HorizontalDivider(color = BorderMetal, modifier = Modifier.padding(vertical = 8.dp))

                // Actions buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            onToggleFavorite()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favori",
                            tint = if (game.isFavorite) FavoriteGold else TextSecondary
                        )
                    }

                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DÉMARRER", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = ButtonRed)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(0.35f))
        Text(
            value,
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.65f)
        )
    }
}

fun formatPlaytime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes} min"
        else -> "Quelques secondes"
    }
}


// --- SCREEN 2: EMULATOR PLAY SCREEN (THE CORE PLAYGROUND) ---

@Composable
fun PlayScreen(viewModel: EmulatorViewModel) {
    val context = LocalContext.current
    val game by viewModel.activeGame.collectAsState()
    val graphics by viewModel.graphicsSettings.collectAsState()
    val audio by viewModel.audioSettings.collectAsState()
    val controller by viewModel.controllerSettings.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val ram by viewModel.ramUsage.collectAsState()
    val gpu by viewModel.gpuLoad.collectAsState()
    val saveStates by viewModel.saveStates.collectAsState()
    val customButtonOffsets by viewModel.buttonOffsets.collectAsState()

    var showMenuOverlay by remember { mutableStateOf(false) }
    var selectedSaveSlot by remember { mutableStateOf(1) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    
    // Virtual game state variables (shared across play mode)
    // Flappy Bird specific state
    var birdY by remember { mutableStateOf(200f) }
    var birdVelocity by remember { mutableStateOf(0f) }
    var flappyScore by remember { mutableStateOf(0) }
    var flappyGameOver by remember { mutableStateOf(false) }
    var flappyPipes = remember { mutableStateListOf<Pair<Float, Float>>() } // x, gapY
    
    // Wipeout specific state
    var shipX by remember { mutableStateOf(0f) } // -100 to 100
    var wipeoutSpeed by remember { mutableStateOf(40f) }
    var wipeoutTrackOffset by remember { mutableStateOf(0f) }
    var wipeoutScore by remember { mutableStateOf(0) }
    var boostActive by remember { mutableStateOf(false) }

    // Physics Sandbox specific state
    var cubeRotX by remember { mutableStateOf(0f) }
    var cubeRotY by remember { mutableStateOf(0f) }
    var physicsMass by remember { mutableStateOf(5f) }

    // Button holds states
    var buttonXPressed by remember { mutableStateOf(false) }
    var buttonOPressed by remember { mutableStateOf(false) }
    var buttonSTPressed by remember { mutableStateOf(false) }
    var buttonTRPressed by remember { mutableStateOf(false) }
    var dpadUpPressed by remember { mutableStateOf(false) }
    var dpadDownPressed by remember { mutableStateOf(false) }
    var dpadLeftPressed by remember { mutableStateOf(false) }
    var dpadRightPressed by remember { mutableStateOf(false) }
    var bumperLPressed by remember { mutableStateOf(false) }
    var bumperRPressed by remember { mutableStateOf(false) }
    
    // Game loop simulation coroutine
    LaunchedEffect(game) {
        if (game == null) return@LaunchedEffect
        // Reset game data on load
        birdY = 200f
        birdVelocity = 0f
        flappyScore = 0
        flappyGameOver = false
        flappyPipes.clear()
        flappyPipes.add(Pair(500f, 250f))
        flappyPipes.add(Pair(850f, 180f))

        shipX = 0f
        wipeoutSpeed = 40f
        wipeoutScore = 0

        while (true) {
            delay(16) // ~60fps simulation loop
            
            val isFlappy = game?.title?.contains("Flappy") == true
            val isWipeout = game?.title?.contains("Wipeout") == true || game?.title?.contains("Ridge") == true
            val isSandbox = game?.title?.contains("Cube") == true || game?.title?.contains("Sandbox") == true

            if (isFlappy && !flappyGameOver) {
                birdVelocity += 0.35f // Gravity
                birdY += birdVelocity

                // Update pipes
                for (i in flappyPipes.indices) {
                    val pipe = flappyPipes[i]
                    val newX = pipe.first - 3f
                    if (newX < -60f) {
                        flappyPipes[i] = Pair(700f, (120..280).random().toFloat())
                        flappyScore++
                    } else {
                        flappyPipes[i] = Pair(newX, pipe.second)
                    }
                }

                // Collisions (using simulated bounds)
                if (birdY < 20f || birdY > 480f) {
                    flappyGameOver = true
                }
                for (pipe in flappyPipes) {
                    if (pipe.first > 120f && pipe.first < 180f) {
                        if (birdY < pipe.second - 65f || birdY > pipe.second + 65f) {
                            flappyGameOver = true
                            triggerVibration(context, 1.0f)
                        }
                    }
                }
            }

            if (isWipeout) {
                wipeoutTrackOffset = (wipeoutTrackOffset + wipeoutSpeed * 0.1f) % 500f
                wipeoutScore += (wipeoutSpeed / 10).toInt()
                
                // Acceleration & drift controls
                if (buttonXPressed) {
                    wipeoutSpeed = (wipeoutSpeed + 1.2f).coerceAtMost(if (boostActive) 160f else 110f)
                } else {
                    wipeoutSpeed = (wipeoutSpeed - 0.5f).coerceAtLeast(30f)
                }

                if (dpadLeftPressed || bumperLPressed) {
                    shipX = (shipX - 4f).coerceAtLeast(-120f)
                }
                if (dpadRightPressed || bumperRPressed) {
                    shipX = (shipX + 4f).coerceAtMost(120f)
                }
                if (!dpadLeftPressed && !dpadRightPressed && !bumperLPressed && !bumperRPressed) {
                    shipX *= 0.9f // Return to center
                }
            }

            if (isSandbox) {
                val spinMultiplier = when (controller.turboSpeed) {
                    true -> 3.5f
                    false -> 1f
                }
                if (dpadUpPressed) cubeRotX -= 2.5f * spinMultiplier
                if (dpadDownPressed) cubeRotX += 2.5f * spinMultiplier
                if (dpadLeftPressed) cubeRotY -= 2.5f * spinMultiplier
                if (dpadRightPressed) cubeRotY += 2.5f * spinMultiplier

                // If not actively controlled, slow rotational drift
                if (!dpadUpPressed && !dpadDownPressed && !dpadLeftPressed && !dpadRightPressed) {
                    cubeRotX += 0.5f
                    cubeRotY += 0.6f
                }
            }
        }
    }

    // Capture alerts
    LaunchedEffect(alertMessage) {
        if (alertMessage != null) {
            delay(1500)
            alertMessage = null
        }
    }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- 1. THE GAME EMULATOR COMPONENT (CANVAS GAMEPLAY) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (showMenuOverlay) 12.dp else 0.dp)
            ) {
                AndroidEmulatorRenderer(
                    game = game,
                    graphics = graphics,
                    birdY = birdY,
                    flappyPipes = flappyPipes,
                    flappyScore = flappyScore,
                    flappyGameOver = flappyGameOver,
                    shipX = shipX,
                    wipeoutTrackOffset = wipeoutTrackOffset,
                    wipeoutSpeed = wipeoutSpeed,
                    wipeoutScore = wipeoutScore,
                    cubeRotX = cubeRotX,
                    cubeRotY = cubeRotY,
                    boostActive = boostActive
                )
            }

            // --- 2. FLOATING REAL-TIME FPS & SYSTEM BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$fps FPS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (fps >= 58) AccentNeon else if (fps >= 30) FavoriteGold else ButtonRed,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "GPU: $gpu", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "RAM: $ram", fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Screenshot
                    IconButton(
                        onClick = {
                            viewModel.captureScreen(game?.title ?: "PSP")
                            triggerVibration(context, 0.4f)
                            alertMessage = "Capture d'écran enregistrée !"
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Screenshot", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    
                    // Main Menu Selector Overlay Toggle
                    IconButton(
                        onClick = { showMenuOverlay = !showMenuOverlay },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("emulator_menu_button")
                    ) {
                        Icon(
                            imageVector = if (showMenuOverlay) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = "Menu Emulation",
                            tint = PrimaryNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- 3. VIRTUAL CONTROLLER OVERLAY ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (showMenuOverlay) 0.1f else 1f)
            ) {
                VirtualGamepadOverlay(
                    settings = controller,
                    customOffsets = customButtonOffsets,
                    onDpadAction = { up, down, left, right ->
                        dpadUpPressed = up
                        dpadDownPressed = down
                        dpadLeftPressed = left
                        dpadRightPressed = right
                    },
                    onButtonXAction = { pressed ->
                        buttonXPressed = pressed
                        // Flappy Bird Jump action on click
                        if (pressed && game?.title?.contains("Flappy") == true && !flappyGameOver) {
                            birdVelocity = -7f
                            triggerVibration(context, 0.5f)
                        } else if (pressed && flappyGameOver) {
                            // Restart Flappy Bird
                            birdY = 200f
                            birdVelocity = 0f
                            flappyScore = 0
                            flappyGameOver = false
                            flappyPipes.clear()
                            flappyPipes.add(Pair(500f, 250f))
                            flappyPipes.add(Pair(850f, 180f))
                        }
                    },
                    onButtonOAction = { pressed ->
                        buttonOPressed = pressed
                        // Activate boost on Wipeout
                        if (pressed && game?.title?.contains("Wipeout") == true) {
                            boostActive = true
                            triggerVibration(context, 1.0f)
                        } else {
                            boostActive = false
                        }
                    },
                    onButtonSTAction = { pressed -> buttonSTPressed = pressed },
                    onButtonTRAction = { pressed -> buttonTRPressed = pressed },
                    onBumperLAction = { pressed -> bumperLPressed = pressed },
                    onBumperRAction = { pressed -> bumperRPressed = pressed }
                )
            }

            // --- 4. QUICK SAVE / LOAD MENU OVERLAY ---
            AnimatedVisibility(
                visible = showMenuOverlay,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .pointerInput(Unit) { detectTapGestures { showMenuOverlay = false } },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .wrapContentHeight()
                            .pointerInput(Unit) { /* intercept clicks */ },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
                        border = BorderStroke(1.dp, BorderMetal)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "MENU DE L'ÉMULATEUR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Save Slot Selector
                            Text("Sélectionner l'emplacement de sauvegarde :", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..5).forEach { slot ->
                                    val hasSave = saveStates.any { it.slot == slot }
                                    OutlinedButton(
                                        onClick = { selectedSaveSlot = slot },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selectedSaveSlot == slot) PrimaryNeon else if (hasSave) SurfaceSteel else Color.Transparent
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selectedSaveSlot == slot) PrimaryNeon else if (hasSave) AccentNeon else BorderMetal
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Text(
                                            text = "$slot",
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedSaveSlot == slot) BackgroundAbyss else if (hasSave) AccentNeon else TextPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Save / Load Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.createSaveState(selectedSaveSlot, "Save Slot $selectedSaveSlot")
                                        triggerVibration(context, 0.8f)
                                        alertMessage = "État sauvegardé sur l'emplacement $selectedSaveSlot"
                                        showMenuOverlay = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentNeon),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save", tint = BackgroundAbyss)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SAUVEGARDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BackgroundAbyss)
                                }

                                Button(
                                    onClick = {
                                        viewModel.loadSaveState(selectedSaveSlot) {
                                            // Trigger game restoration loads
                                            if (game?.title?.contains("Flappy") == true) {
                                                birdY = (150..280).random().toFloat()
                                                flappyGameOver = false
                                            } else if (game?.title?.contains("Wipeout") == true) {
                                                wipeoutSpeed = 85f
                                            }
                                        }
                                        triggerVibration(context, 0.8f)
                                        alertMessage = "État rechargé !"
                                        showMenuOverlay = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    enabled = saveStates.any { it.slot == selectedSaveSlot }
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Load", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("RECHARGER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = BorderMetal)
                            Spacer(modifier = Modifier.height(16.dp))

                            // General Actions
                            Button(
                                onClick = {
                                    showMenuOverlay = false
                                    viewModel.navigateTo(Screen.Settings)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceSteel),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Quick Adjust", tint = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ajustements Graphiques & Audio", color = TextPrimary, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.exitGame() },
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("exit_game_button")
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Quitter", tint = TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("QUITTER LA SESSION PSP", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // --- 5. FLOATING DYNAMIC ALERT DIALOG BANNER ---
            AnimatedVisibility(
                visible = alertMessage != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                alertMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentNeon),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = BackgroundAbyss,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- CORE SIMULATOR GAMEPLAY RENDERER ---
@Composable
fun AndroidEmulatorRenderer(
    game: Game?,
    graphics: GraphicsSettings,
    birdY: Float,
    flappyPipes: List<Pair<Float, Float>>,
    flappyScore: Int,
    flappyGameOver: Boolean,
    shipX: Float,
    wipeoutTrackOffset: Float,
    wipeoutSpeed: Float,
    wipeoutScore: Int,
    cubeRotX: Float,
    cubeRotY: Float,
    boostActive: Boolean
) {
    val isFlappy = game?.title?.contains("Flappy") == true
    val isWipeout = game?.title?.contains("Wipeout") == true || game?.title?.contains("Ridge") == true
    val isSandbox = game?.title?.contains("Cube") == true || game?.title?.contains("Sandbox") == true

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Background color of emulation screen
        drawRect(Color(0xFF020408))

        if (isFlappy) {
            // Render Flappy Bird Simulation Game Frame
            // Sky & Clouds
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF60A5FA))
                ),
                size = size
            )

            // Draw pipes
            for (pipe in flappyPipes) {
                // Top pipe
                drawRect(
                    color = Color(0xFF22C55E),
                    topLeft = Offset(pipe.first, 0f),
                    size = Size(60f, pipe.second - 65f)
                )
                // Bottom pipe
                drawRect(
                    color = Color(0xFF15803D),
                    topLeft = Offset(pipe.first, pipe.second + 65f),
                    size = Size(60f, height - (pipe.second + 65f))
                )
            }

            // Draw flappy bird
            drawCircle(
                color = FavoriteGold,
                radius = 16f,
                center = Offset(150f, birdY)
            )
            // Eye
            drawCircle(
                color = Color.Black,
                radius = 3f,
                center = Offset(156f, birdY - 4f)
            )
            // Beak
            val beakPath = Path().apply {
                moveTo(164f, birdY - 2f)
                lineTo(174f, birdY + 2f)
                lineTo(164f, birdY + 6f)
                close()
            }
            drawPath(beakPath, Color(0xFFF97316))

            // Score HUD
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 30f,
                center = Offset(width / 2, 80f)
            )

            if (flappyGameOver) {
                drawRect(Color.Black.copy(alpha = 0.65f))
                // Game Over text details
                // Handled in canvas native layout
            }
        }

        if (isWipeout) {
            // Render Wipeout futuristic racer loop
            // Space grid background
            drawRect(Color(0xFF04060F))

            // Star trails
            for (i in 0..15) {
                val starY = ((i * 45 + wipeoutTrackOffset * 0.8f) % height)
                val starX = (i * 92) % width
                drawLine(
                    color = PrimaryNeon.copy(alpha = 0.25f),
                    start = Offset(starX, starY),
                    end = Offset(starX, starY + 25f),
                    strokeWidth = 1.5f
                )
            }

            // Tubular Futuristic Track (converging lines)
            val horizonY = height * 0.35f
            val centerX = width / 2

            // Track boundaries
            val trackLPath = Path().apply {
                moveTo(centerX - 40f, horizonY)
                lineTo(0f, height)
                lineTo(250f, height)
                lineTo(centerX - 20f, horizonY)
                close()
            }
            val trackRPath = Path().apply {
                moveTo(centerX + 40f, horizonY)
                lineTo(width, height)
                lineTo(width - 250f, height)
                lineTo(centerX + 20f, horizonY)
                close()
            }
            drawPath(trackLPath, Color(0xFF0F172A))
            drawPath(trackRPath, Color(0xFF0F172A))

            // Road stripes
            val numStripes = 6
            for (i in 0 until numStripes) {
                val progress = ((i * (height / numStripes) + wipeoutTrackOffset) % height) / height
                val stripeY = horizonY + (height - horizonY) * progress
                val stripeW = 100f * progress + 10f
                drawLine(
                    color = if (boostActive) AccentNeon else PrimaryNeon,
                    start = Offset(centerX - stripeW, stripeY),
                    end = Offset(centerX + stripeW, stripeY),
                    strokeWidth = 3f * progress + 1f
                )
            }

            // Wipeout Racer Ship
            val shipY = height * 0.75f
            val curShipX = centerX + shipX
            
            val shipPath = Path().apply {
                moveTo(curShipX, shipY - 15f) // Nose
                lineTo(curShipX - 25f, shipY + 25f) // Left Wing
                lineTo(curShipX - 10f, shipY + 15f) // Left thruster inner
                lineTo(curShipX + 10f, shipY + 15f) // Right thruster inner
                lineTo(curShipX + 25f, shipY + 25f) // Right Wing
                close()
            }
            drawPath(shipPath, Color(0xFFE2E8F0))
            drawPath(shipPath, if (boostActive) AccentNeon else PrimaryNeon, style = Stroke(2f))

            // Thruster fire glow
            drawCircle(
                color = if (boostActive) AccentNeon.copy(alpha = 0.8f) else ButtonRed.copy(alpha = 0.8f),
                radius = (10..22).random().toFloat(),
                center = Offset(curShipX, shipY + 22f)
            )
        }

        if (isSandbox) {
            // Renders rotating 3D looking Cube Sandbox
            drawRect(Color(0xFF020306))

            // Cyber matrix digital network
            for (i in 0..8) {
                val lx = i * (width / 8)
                drawLine(Color(0xFF1E293B).copy(alpha = 0.3f), Offset(lx, 0f), Offset(lx, height), 1f)
            }
            for (i in 0..6) {
                val ly = i * (height / 6)
                drawLine(Color(0xFF1E293B).copy(alpha = 0.3f), Offset(0f, ly), Offset(width, ly), 1f)
            }

            // Draw a spinning interactive wireframe cube
            val cx = width / 2
            val cy = height / 2
            val size = 180f

            // Calculate rotated coordinates of 8 cube vertices
            val vertices = arrayOf(
                rotate3D(-1f, -1f, -1f, cubeRotX, cubeRotY),
                rotate3D(1f, -1f, -1f, cubeRotX, cubeRotY),
                rotate3D(1f, 1f, -1f, cubeRotX, cubeRotY),
                rotate3D(-1f, 1f, -1f, cubeRotX, cubeRotY),
                rotate3D(-1f, -1f, 1f, cubeRotX, cubeRotY),
                rotate3D(1f, -1f, 1f, cubeRotX, cubeRotY),
                rotate3D(1f, 1f, 1f, cubeRotX, cubeRotY),
                rotate3D(-1f, 1f, 1f, cubeRotX, cubeRotY)
            )

            val px = vertices.map { Offset(cx + it.first * size, cy + it.second * size) }

            // Connect lines
            val edges = arrayOf(
                Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0), // Back
                Pair(4, 5), Pair(5, 6), Pair(6, 7), Pair(7, 4), // Front
                Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7)  // Connections
            )

            for (edge in edges) {
                drawLine(
                    color = PrimaryNeon,
                    start = px[edge.first],
                    end = px[edge.second],
                    strokeWidth = 3f
                )
            }

            // Glowing nodes
            for (p in px) {
                drawCircle(AccentNeon, 6f, p)
            }
        }

        // --- GRAPHICS POST-PROCESSING SHADER EMULATION OVERLAYS ---
        when (graphics.activeShader) {
            "CRT-Geom (Scanlines)", "Scanlines" -> {
                // Emulate CRT horizontal scanlines across canvas
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 2f
                    )
                    y += 4f
                }
            }
            "GameBoy" -> {
                // Monochromatic matrix overlay
                drawRect(Color(0xFF8BAC0F).copy(alpha = 0.3f))
            }
            "Monochrome" -> {
                // Soft black and white tint
                drawRect(Color.White.copy(alpha = 0.1f))
            }
            else -> {}
        }
    }

    // Dynamic HUD rendering in Compose (not canvas lines) to keep clean code
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isFlappy && flappyGameOver) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "GAME OVER",
                    color = Color.Red,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Score : $flappyScore",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Restart triggers in input controller click */ },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("PRESS (X) TO RESTART", color = BackgroundAbyss, fontWeight = FontWeight.Black)
                }
            }
        }

        if (isWipeout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp, top = 80.dp)
            ) {
                // Speed and score dashboard
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("VITESSE", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "${wipeoutSpeed.toInt()} km/h",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (boostActive) AccentNeon else PrimaryNeon,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("POINTS", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "$wipeoutScore",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// Helper rotating formula for 3D simulation
fun rotate3D(x: Float, y: Float, z: Float, angleX: Float, angleY: Float): Pair<Float, Float> {
    val radX = Math.toRadians(angleX.toDouble())
    val radY = Math.toRadians(angleY.toDouble())

    // Rotate X axis
    val cosX = cos(radX).toFloat()
    val sinX = sin(radX).toFloat()
    val y1 = y * cosX - z * sinX
    val z1 = y * sinX + z * cosX

    // Rotate Y axis
    val cosY = cos(radY).toFloat()
    val sinY = sin(radY).toFloat()
    val x2 = x * cosY + z1 * sinY
    // Projection simple perspective
    val depth = 3.0f
    val scale = 1.0f / (depth - x2 * 0.2f)

    return Pair(x2 * scale, y1 * scale)
}


// --- SCREEN 3: CONTROLLERS LAYOUT OVERLAY ---

@Composable
fun VirtualGamepadOverlay(
    settings: ControllerSettings,
    customOffsets: Map<String, Offset>,
    onDpadAction: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onButtonXAction: (Boolean) -> Unit,
    onButtonOAction: (Boolean) -> Unit,
    onButtonSTAction: (Boolean) -> Unit,
    onButtonTRAction: (Boolean) -> Unit,
    onBumperLAction: (Boolean) -> Unit,
    onBumperRAction: (Boolean) -> Unit
) {
    val opacity = settings.opacity
    val scale = settings.scale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { /* don't let clicks go behind buttons easily */ }
            .alpha(opacity)
    ) {
        // 1. LEFT BUMPER (L)
        val lOffset = customOffsets["L"] ?: Offset(20f, 60f)
        Box(
            modifier = Modifier
                .offset { IntOffset(lOffset.x.toInt(), lOffset.y.toInt()) }
                .size((80 * scale).dp, (36 * scale).dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onBumperLAction(true)
                            try { awaitRelease() } finally { onBumperLAction(false) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("L", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // 2. RIGHT BUMPER (R)
        val rOffset = customOffsets["R"] ?: Offset(1200f, 60f) // Fallbacks adjusted dynamically in Compose with Alignments usually
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp, top = 20.dp) // Offset default using align helper
                .size((80 * scale).dp, (36 * scale).dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onBumperRAction(true)
                            try { awaitRelease() } finally { onBumperRAction(false) }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("R", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // 3. D-PAD (DIRECTIONS LEFT)
        val dpadSize = (140 * scale).dp
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 120.dp)
                .size(dpadSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        ) {
            // Directional Buttons nested inside circular Dpad
            // UP
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size((42 * scale).dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onDpadAction(true, false, false, false); try { awaitRelease() } finally { onDpadAction(false, false, false, false) } })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowDropUp, contentDescription = "Up", tint = TextPrimary)
            }
            // DOWN
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size((42 * scale).dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onDpadAction(false, true, false, false); try { awaitRelease() } finally { onDpadAction(false, false, false, false) } })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Down", tint = TextPrimary)
            }
            // LEFT
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size((42 * scale).dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onDpadAction(false, false, true, false); try { awaitRelease() } finally { onDpadAction(false, false, false, false) } })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowLeft, contentDescription = "Left", tint = TextPrimary)
            }
            // RIGHT
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size((42 * scale).dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onDpadAction(false, false, false, true); try { awaitRelease() } finally { onDpadAction(false, false, false, false) } })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowRight, contentDescription = "Right", tint = TextPrimary)
            }
        }

        // 4. ACTION BUTTONS (TRIANGLE, CIRCLE, CROSS, SQUARE RIGHT)
        val actionsSize = (140 * scale).dp
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 120.dp)
                .size(actionsSize)
        ) {
            // TRIANGLE (Green)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size((42 * scale).dp)
                    .clip(CircleShape)
                    .background(ButtonGreen.copy(alpha = 0.4f))
                    .border(1.dp, ButtonGreen, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onButtonTRAction(true); try { awaitRelease() } finally { onButtonTRAction(false) } })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("▲", color = ButtonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            // CIRCLE (Red)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size((42 * scale).dp)
                    .clip(CircleShape)
                    .background(ButtonRed.copy(alpha = 0.4f))
                    .border(1.dp, ButtonRed, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onButtonOAction(true); try { awaitRelease() } finally { onButtonOAction(false) } })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("●", color = ButtonRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            // CROSS (Blue)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size((42 * scale).dp)
                    .clip(CircleShape)
                    .background(ButtonBlue.copy(alpha = 0.4f))
                    .border(1.dp, ButtonBlue, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onButtonXAction(true); try { awaitRelease() } finally { onButtonXAction(false) } })
                    }
                    .testTag("action_button_cross"),
                contentAlignment = Alignment.Center
            ) {
                Text("✖", color = ButtonBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            // SQUARE (Orange/Pink)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size((42 * scale).dp)
                    .clip(CircleShape)
                    .background(ButtonOrange.copy(alpha = 0.4f))
                    .border(1.dp, ButtonOrange, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onButtonSTAction(true); try { awaitRelease() } finally { onButtonSTAction(false) } })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("■", color = ButtonOrange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 5. ANALOG JOYSTICK
        if (settings.showAnalogStick) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 20.dp)
                    .size((64 * scale).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                // Joystick Cap
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size((38 * scale).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.5f), Color.DarkGray)
                            )
                        )
                )
            }
        }

        // 6. SELECT & START BUTTONS
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SELECT
            Box(
                modifier = Modifier
                    .size((60 * scale).dp, (24 * scale).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text("SELECT", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // START
            Box(
                modifier = Modifier
                    .size((60 * scale).dp, (24 * scale).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text("START", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// --- SCREEN 4: CUSTOMIZE TOUCH BUTTONS SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeControlsScreen(viewModel: EmulatorViewModel) {
    val context = LocalContext.current
    val controller by viewModel.controllerSettings.collectAsState()
    val customButtonOffsets by viewModel.buttonOffsets.collectAsState()

    var activeDpadOffset by remember { mutableStateOf(Offset(100f, 400f)) }
    var activeActionsOffset by remember { mutableStateOf(Offset(900f, 400f)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Édition Personnalisée des Contrôles", fontSize = 16.sp, fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundAbyss)
            )
        },
        containerColor = BackgroundAbyss
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Instructions Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
                border = BorderStroke(1.dp, BorderMetal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = "Glissez-déposez les blocs de manette n'importe où sur l'écran pour configurer votre disposition de jeu personnalisée.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // MOCK CONTROLLER POSITION 1: D-PAD
            Box(
                modifier = Modifier
                    .offset { IntOffset(activeDpadOffset.x.toInt(), activeDpadOffset.y.toInt()) }
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(PrimaryNeon.copy(alpha = 0.2f))
                    .border(2.dp, PrimaryNeon, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            activeDpadOffset += dragAmount
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("D-PAD\n(Glisser)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            // MOCK CONTROLLER POSITION 2: ACTIONS
            Box(
                modifier = Modifier
                    .offset { IntOffset(activeActionsOffset.x.toInt(), activeActionsOffset.y.toInt()) }
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(PrimaryNeon.copy(alpha = 0.2f))
                    .border(2.dp, PrimaryNeon, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            activeActionsOffset += dragAmount
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("BOUTONS\n(Glisser)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            // Bottom control buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        activeDpadOffset = Offset(100f, 400f)
                        activeActionsOffset = Offset(900f, 400f)
                        viewModel.resetButtonOffsets()
                        triggerVibration(context, 0.5f)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceSteel),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Réinitialiser")
                }

                Button(
                    onClick = {
                        // Persist layout settings
                        triggerVibration(context, 1.0f)
                        viewModel.navigateTo(Screen.Settings)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enregistrer", color = BackgroundAbyss, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// --- SCREEN 5: SETTINGS SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: EmulatorViewModel) {
    val context = LocalContext.current
    val graphics by viewModel.graphicsSettings.collectAsState()
    val audio by viewModel.audioSettings.collectAsState()
    val controller by viewModel.controllerSettings.collectAsState()
    val profile by viewModel.performanceProfile.collectAsState()

    var activeCategory by remember { mutableStateOf("Graphismes") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres Avancés", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Library) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundAbyss)
            )
        },
        containerColor = BackgroundAbyss
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Sidebar (For Tablet compliance & beautiful layout)
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .fillMaxHeight()
                    .background(SurfaceMetal)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val categories = listOf("Graphismes", "Audio", "Contrôles", "Optimisation", "Notifications")
                categories.forEach { cat ->
                    val active = activeCategory == cat
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeCategory = cat }
                            .background(if (active) SurfaceSteel else Color.Transparent)
                            .padding(vertical = 14.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cat,
                            color = if (active) PrimaryNeon else TextSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Settings Fields Column
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        activeCategory.uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = BorderMetal)
                }

                when (activeCategory) {
                    "Graphismes" -> {
                        item {
                            SettingsDropdown(
                                label = "Pilote de Rendu API",
                                current = graphics.renderer.displayName,
                                options = listOf("Vulkan 1.3 (Recommandé)", "OpenGL ES 3.2"),
                                onSelected = {
                                    val r = if (it.contains("Vulkan")) RendererType.Vulkan else RendererType.OpenGLES
                                    viewModel.updateGraphicsSettings { old -> old.copy(renderer = r) }
                                }
                            )
                        }
                        item {
                            SettingsSlider(
                                label = "Résolution Interne du Noyau",
                                value = graphics.resolutionScale.toFloat(),
                                range = 1f..10f,
                                steps = 9,
                                valueDisplay = "${graphics.resolutionScale}x PSP",
                                onValueChange = {
                                    viewModel.updateGraphicsSettings { old -> old.copy(resolutionScale = it.toInt()) }
                                }
                            )
                        }
                        item {
                            SettingsSwitch(
                                label = "FPS Illimité (Déverrouiller)",
                                description = "Emule au-delà de 30/60 fps d'origine si le matériel le permet.",
                                checked = graphics.fpsUnlock,
                                onCheckedChange = {
                                    viewModel.updateGraphicsSettings { old -> old.copy(fpsUnlock = it) }
                                }
                            )
                        }
                        item {
                            SettingsDropdown(
                                label = "Gestionnaire de Shaders",
                                current = graphics.activeShader,
                                options = listOf("CRT-Geom (Scanlines)", "GameBoy", "Monochrome", "Scanlines", "VHS", "None"),
                                onSelected = {
                                    viewModel.updateGraphicsSettings { old -> old.copy(activeShader = it) }
                                }
                            )
                        }
                        item {
                            SettingsSwitch(
                                label = "Anti-Aliasing (FXAA)",
                                description = "Lisse les contours 3D géométriques.",
                                checked = graphics.antiAliasing,
                                onCheckedChange = {
                                    viewModel.updateGraphicsSettings { old -> old.copy(antiAliasing = it) }
                                }
                            )
                        }
                    }

                    "Audio" -> {
                        item {
                            SettingsSwitch(
                                label = "Faible Latence Audio",
                                description = "Synchronisation en temps réel pour éviter le déphasage.",
                                checked = audio.lowLatency,
                                onCheckedChange = {
                                    viewModel.updateAudioSettings { old -> old.copy(lowLatency = it) }
                                }
                            )
                        }
                        item {
                            SettingsSwitch(
                                label = "Son Stéréo",
                                description = "Espace sonore élargi sur 2 canaux distincts.",
                                checked = audio.stereo,
                                onCheckedChange = {
                                    viewModel.updateAudioSettings { old -> old.copy(stereo = it) }
                                }
                            )
                        }
                        item {
                            SettingsSlider(
                                label = "Volume Général",
                                value = audio.volume,
                                range = 0f..1f,
                                valueDisplay = "${(audio.volume * 100).toInt()}%",
                                onValueChange = {
                                    viewModel.updateAudioSettings { old -> old.copy(volume = it) }
                                }
                            )
                        }
                    }

                    "Contrôles" -> {
                        item {
                            SettingsSlider(
                                label = "Opacité de la Manette Tactile",
                                value = controller.opacity,
                                range = 0.1f..1.0f,
                                valueDisplay = "${(controller.opacity * 100).toInt()}%",
                                onValueChange = {
                                    viewModel.updateControllerSettings { old -> old.copy(opacity = it) }
                                }
                            )
                        }
                        item {
                            SettingsSlider(
                                label = "Taille Globale de la Manette",
                                value = controller.scale,
                                range = 0.6f..1.5f,
                                valueDisplay = "Multiplicateur: ${"%.1f".format(controller.scale)}x",
                                onValueChange = {
                                    viewModel.updateControllerSettings { old -> old.copy(scale = it) }
                                }
                            )
                        }
                        item {
                            SettingsSwitch(
                                label = "Tactile Vibrant (Haptique)",
                                description = "Vibre lors de la pression des touches virtuelles.",
                                checked = controller.vibrationStrength > 0f,
                                onCheckedChange = {
                                    val strength = if (it) 0.6f else 0f
                                    viewModel.updateControllerSettings { old -> old.copy(vibrationStrength = strength) }
                                }
                            )
                        }
                        item {
                            SettingsSwitch(
                                label = "Mode Turbo Actif",
                                description = "Accélère les entrées de touches en continu.",
                                checked = controller.turboSpeed,
                                onCheckedChange = {
                                    viewModel.updateControllerSettings { old -> old.copy(turboSpeed = it) }
                                }
                            )
                        }
                        item {
                            Button(
                                onClick = { viewModel.navigateTo(Screen.CustomizeControls) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .testTag("reposition_controls_button")
                            ) {
                                Icon(Icons.Default.OpenWith, contentDescription = "Move", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Repositionner les touches", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    "Optimisation" -> {
                        item {
                            Text(
                                "PROFIL D'OPTIMISATION DU SYSTÈME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                        PerformanceProfile.values().forEach { prof ->
                            val active = profile == prof
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updatePerformanceProfile(prof) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (active) SurfaceSteel else SurfaceMetal),
                                    border = BorderStroke(1.dp, if (active) PrimaryNeon else BorderMetal)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(prof.displayName, fontWeight = FontWeight.Bold, color = if (active) PrimaryNeon else TextPrimary)
                                            if (active) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Fréquence CPU : ${prof.cpuFreq}", fontSize = 11.sp, color = TextSecondary)
                                        Text("Batterie Estimée : ${prof.batteryUsage}", fontSize = 11.sp, color = TextSecondary)
                                        Text("Température : ${prof.temp}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    "Notifications" -> {
                        item {
                            Text(
                                "CATÉGORIES DE NOTIFICATIONS NATIVES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        item {
                            val notifyScans = remember { mutableStateOf(viewModel.notificationHelper.isCategoryEnabled(com.example.core.NotificationHelper.CHANNEL_SCANS)) }
                            SettingsSwitch(
                                label = "Bibliothèque & Scans",
                                description = "Notifier lorsqu'un scan du stockage détecte de nouveaux fichiers de jeux ISO/CSO/PBP.",
                                checked = notifyScans.value,
                                onCheckedChange = {
                                    notifyScans.value = it
                                    viewModel.notificationHelper.setCategoryEnabled(com.example.core.NotificationHelper.CHANNEL_SCANS, it)
                                }
                            )
                        }
                        item {
                            val notifySaves = remember { mutableStateOf(viewModel.notificationHelper.isCategoryEnabled(com.example.core.NotificationHelper.CHANNEL_SAVES)) }
                            SettingsSwitch(
                                label = "Sauvegardes Émulation",
                                description = "Notifier pour confirmer le succès de la sauvegarde rapide (Save States).",
                                checked = notifySaves.value,
                                onCheckedChange = {
                                    notifySaves.value = it
                                    viewModel.notificationHelper.setCategoryEnabled(com.example.core.NotificationHelper.CHANNEL_SAVES, it)
                                }
                            )
                        }
                        item {
                            val notifyPerf = remember { mutableStateOf(viewModel.notificationHelper.isCategoryEnabled(com.example.core.NotificationHelper.CHANNEL_PERF)) }
                            SettingsSwitch(
                                label = "Optimisation & Performance",
                                description = "Notifier lors d'un changement de profil de performance ou de mode turbo.",
                                checked = notifyPerf.value,
                                onCheckedChange = {
                                    notifyPerf.value = it
                                    viewModel.notificationHelper.setCategoryEnabled(com.example.core.NotificationHelper.CHANNEL_PERF, it)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SETTINGS HELPER COMPONENT: SWITCH ---
@Composable
fun SettingsSwitch(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.8f)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text(description, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundAbyss,
                checkedTrackColor = PrimaryNeon,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceSteel
            )
        )
    }
}

// --- SETTINGS HELPER COMPONENT: SLIDER ---
@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueDisplay: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text(valueDisplay, fontSize = 13.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                activeTrackColor = PrimaryNeon,
                inactiveTrackColor = SurfaceSteel,
                thumbColor = PrimaryNeon
            )
        )
    }
}

// --- SETTINGS HELPER COMPONENT: DROPDOWN ---
@Composable
fun SettingsDropdown(label: String, current: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceMetal)
                .border(1.dp, BorderMetal, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(current, color = TextPrimary, fontSize = 13.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .background(SurfaceMetal)
                    .border(1.dp, BorderMetal)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// --- SCREEN: INTEGRATED GLOBAL SAVE STATES MANAGER SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveStatesManagerScreen(viewModel: EmulatorViewModel) {
    val allSaves by viewModel.allSaveStates.collectAsState()
    val allGames by viewModel.allGames.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val filteredSaves = remember(allSaves, searchQuery) {
        if (searchQuery.isBlank()) {
            allSaves
        } else {
            allSaves.filter {
                it.gameTitle.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GESTIONNAIRE DE SAUVEGARDES", fontSize = 10.sp, color = PrimaryNeon, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("Sauvegardes Globales", fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundAbyss),
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        Text(
                            text = "${filteredSaves.size} Fichiers",
                            fontSize = 11.sp,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(1.dp, PrimaryNeon.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        },
        containerColor = BackgroundAbyss
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher par jeu ou description...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("saves_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryNeon,
                    unfocusedBorderColor = BorderMetal,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceMetal,
                    unfocusedContainerColor = SurfaceMetal
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredSaves.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAs,
                        contentDescription = "No saves",
                        tint = BorderMetal,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune sauvegarde trouvée",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Modifiez votre recherche." else "Vos sauvegardes 'Save State' apparaîtront ici dès que vous en créerez pendant une partie.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSaves, key = { it.id }) { saveState ->
                        SaveStateCard(
                            saveState = saveState,
                            onDelete = { viewModel.deleteSaveState(saveState) },
                            onLoad = {
                                val game = allGames.find { it.id == saveState.gameId }
                                if (game != null) {
                                    viewModel.selectGameAndPlay(game)
                                    scope.launch {
                                        delay(800)
                                        viewModel.loadSaveState(saveState.slot) {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaveStateCard(
    saveState: SaveState,
    onDelete: () -> Unit,
    onLoad: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderMetal, RoundedCornerShape(16.dp))
            .testTag("save_state_card_${saveState.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceMetal),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .size(90.dp, 65.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceSteel)
                    .border(1.dp, BorderMetal, RoundedCornerShape(8.dp))
            ) {
                if (saveState.thumbnailPath != null && !saveState.thumbnailPath.startsWith("virtual://")) {
                    val painter = coil.compose.rememberAsyncImagePainter(saveState.thumbnailPath)
                    Image(
                        painter = painter,
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else if (saveState.gameCoverUrl != null) {
                    val painter = coil.compose.rememberAsyncImagePainter(saveState.gameCoverUrl)
                    Image(
                        painter = painter,
                        contentDescription = "Cover Fallback",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(PrimaryNeon.copy(alpha = 0.2f), ButtonRed.copy(alpha = 0.2f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "No Preview",
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Slot overlay badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(if (saveState.slot == 0) ButtonRed else PrimaryNeon, RoundedCornerShape(topEnd = 6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (saveState.slot == 0) "AUTO" else "SLOT ${saveState.slot}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = saveState.gameTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = saveState.description,
                    fontSize = 11.sp,
                    color = PrimaryNeon,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = TextSecondary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val dateFormatted = remember(saveState.timestamp) {
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(saveState.timestamp))
                    }
                    Text(
                        text = dateFormatted,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // Quick Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLoad,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Load Save",
                        tint = AccentNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Save",
                        tint = ButtonRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer la sauvegarde ?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Cette action supprimera définitivement cette sauvegarde de l'application.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonRed)
                ) {
                    Text("Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            containerColor = SurfaceMetal
        )
    }
}
