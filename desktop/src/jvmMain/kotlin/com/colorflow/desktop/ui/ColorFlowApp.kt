package com.colorflow.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colorflow.common.logic.GameLogic
import com.colorflow.common.model.*
import com.colorflow.common.storage.GameStorage
import com.colorflow.common.storage.GameSettings
import com.colorflow.common.storage.PlatformStorage

enum class Screen {
    START, GAME, LEVELS, STATS, SETTINGS
}

@Composable
fun ColorFlowApp() {
    val storage = remember { PlatformStorage() }
    val gameStorage = remember { GameStorage(storage) }
    var settings by remember { mutableStateOf(gameStorage.loadSettings()) }
    var currentScreen by remember { mutableStateOf(Screen.START) }
    var gameState by remember { mutableStateOf<GameState?>(null) }
    var stats by remember { mutableStateOf(gameStorage.loadStats()) }
    var completedLevels by remember { mutableStateOf(gameStorage.loadCompletedLevels()) }

    val colorScheme = if (settings.darkTheme) {
        darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF81C784),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1E88E5),
            secondary = Color(0xFF43A047),
            background = Color(0xFFF5F5F5),
            surface = Color.White
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                }
            ) { screen ->
                when (screen) {
                    Screen.START -> StartScreen(
                        onPlayGame = {
                            val (board, dots) = GameLogic.generateBoard()
                            gameState = GameState.create(dots, board)
                            currentScreen = Screen.GAME
                        },
                        onLevels = { currentScreen = Screen.LEVELS },
                        onStats = { currentScreen = Screen.STATS },
                        onSettings = { currentScreen = Screen.SETTINGS }
                    )

                    Screen.GAME -> gameState?.let { state ->
                        GameScreen(
                            gameState = state,
                            settings = settings,
                            onBack = { currentScreen = Screen.START },
                            onStateUpdate = { newState ->
                                gameState = newState
                                if (newState.isWon) {
                                    val newStats = stats.copy(
                                        totalGamesPlayed = stats.totalGamesPlayed + 1,
                                        totalGamesWon = stats.totalGamesWon + 1,
                                        totalTimeMs = stats.totalTimeMs + newState.elapsedTimeMs
                                    )
                                    stats = newStats
                                    gameStorage.saveStats(newStats)
                                }
                            },
                            onHintUsed = { newState ->
                                gameState = newState
                            }
                        )
                    }

                    Screen.LEVELS -> LevelsScreen(
                        completedLevels = completedLevels,
                        onLevelSelected = { level ->
                            val state = GameState.create(level.dots, level.board, level)
                            gameState = state
                            currentScreen = Screen.GAME
                        },
                        onBack = { currentScreen = Screen.START }
                    )

                    Screen.STATS -> StatsScreen(
                        stats = stats,
                        onBack = { currentScreen = Screen.START }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSettingsUpdate = { newSettings ->
                            settings = newSettings
                            gameStorage.saveSettings(newSettings)
                        },
                        onBack = { currentScreen = Screen.START }
                    )
                }
            }
        }
    }
}

@Composable
fun StartScreen(
    onPlayGame: () -> Unit,
    onLevels: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Color Flow",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Connect matching colored dots\nFill the entire board to win!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        val colorDots = listOf(
            CellColor.RED, CellColor.BLUE, CellColor.GREEN,
            CellColor.YELLOW, CellColor.PURPLE, CellColor.ORANGE
        )

        Row(
            modifier = Modifier.padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colorDots.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(color.hexColor))
                )
            }
        }

        Button(
            onClick = onPlayGame,
            modifier = Modifier
                .width(220.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Quick Play",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLevels,
            modifier = Modifier
                .width(220.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Levels",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onStats,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Stats", fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Settings", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    settings: GameSettings,
    onBack: () -> Unit,
    onStateUpdate: (GameState) -> Unit,
    onHintUsed: (GameState) -> Unit
) {
    var elapsedTime by remember { mutableStateOf(0L) }

    LaunchedEffect(gameState.isTimerRunning) {
        if (gameState.isTimerRunning && !gameState.isWon) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                elapsedTime += 1000
                onStateUpdate(gameState.copy(elapsedTimeMs = elapsedTime))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }

            Text(
                text = "Color Flow",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = formatTime(elapsedTime),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Moves: ${gameState.moveCount}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val newState = GameLogic.useHint(gameState)
                        onHintUsed(newState)
                    },
                    enabled = gameState.hintsRemaining > 0 && !gameState.isWon,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7E57C2)
                    )
                ) {
                    Text("Hint (${gameState.hintsRemaining})", color = Color.White)
                }

                Button(
                    onClick = {
                        val newState = GameLogic.undoMove(gameState)
                        onStateUpdate(newState)
                    },
                    enabled = gameState.undoStack.isNotEmpty() && !gameState.isWon,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7043)
                    )
                ) {
                    Text("Undo", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { gameState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        BoardComposable(
            gameState = gameState,
            onDotSelected = { dot ->
                handleDotSelection(gameState, dot, onStateUpdate)
            },
            modifier = Modifier.weight(1f)
        )

        if (gameState.isWon) {
            VictoryScreen(
                moveCount = gameState.moveCount,
                timeMs = elapsedTime,
                levelId = gameState.currentLevel?.id,
                onRestart = onBack
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun handleDotSelection(
    gameState: GameState,
    clickedDot: Dot,
    onStateUpdate: (GameState) -> Unit
) {
    val currentSelected = gameState.selectedDot

    if (currentSelected == null) {
        val newState = gameState.copy(selectedDot = clickedDot, isTimerRunning = true)
        onStateUpdate(newState)
    } else if (currentSelected.pairId == clickedDot.pairId && currentSelected.position != clickedDot.position) {
        val savedState = GameLogic.saveSnapshot(gameState)
        val result = GameLogic.connectDots(savedState, currentSelected, clickedDot)
        if (result != null) {
            onStateUpdate(result)
        } else {
            onStateUpdate(gameState.copy(selectedDot = null))
        }
    } else if (currentSelected.position == clickedDot.position) {
        onStateUpdate(gameState.copy(selectedDot = null))
    } else {
        val existingConn = gameState.connections.find {
            it.dot1.pairId == clickedDot.pairId && it.isComplete
        }
        if (existingConn != null) {
            val clearedState = GameLogic.removeConnection(gameState, clickedDot.pairId)
            val savedState = GameLogic.saveSnapshot(clearedState)
            val newState = savedState.copy(selectedDot = clickedDot)
            onStateUpdate(newState)
        } else {
            onStateUpdate(gameState.copy(selectedDot = clickedDot))
        }
    }
}

@Composable
fun BoardComposable(
    gameState: GameState,
    onDotSelected: (Dot) -> Unit,
    modifier: Modifier = Modifier
) {
    val boardSize = gameState.board.size
    var animationPhase by remember { mutableStateOf(0f) }

    LaunchedEffect(gameState.moveCount) {
        animationPhase = 0f
        while (animationPhase < 1f) {
            animationPhase += 0.02f
            kotlinx.coroutines.delay(16)
        }
        animationPhase = 1f
    }

    val animatedPathProgress by animateFloatAsState(
        targetValue = animationPhase,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / boardSize
            val cellHeight = size.height / boardSize

            for (conn in gameState.connections) {
                if (conn.isComplete && conn.path.isNotEmpty()) {
                    val pathToDraw = Path()
                    val visibleCells = (conn.path.size * animatedPathProgress).toInt().coerceAtLeast(0)

                    for (i in 0 until visibleCells.coerceAtMost(conn.path.size)) {
                        val pos = conn.path[i]
                        val cx = pos.col * cellWidth + cellWidth / 2
                        val cy = pos.row * cellHeight + cellHeight / 2

                        if (i == 0) {
                            pathToDraw.moveTo(cx, cy)
                        } else {
                            pathToDraw.lineTo(cx, cy)
                        }
                    }

                    drawPath(
                        path = pathToDraw,
                        color = Color(conn.dot1.color.hexColor).copy(alpha = 0.7f),
                        style = Stroke(
                            width = cellWidth * 0.3f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            gameState.currentHint?.let { hint ->
                val hintPath = Path()
                for ((i, pos) in hint.path.withIndex()) {
                    val cx = pos.col * cellWidth + cellWidth / 2
                    val cy = pos.row * cellHeight + cellHeight / 2
                    if (i == 0) hintPath.moveTo(cx, cy) else hintPath.lineTo(cx, cy)
                }
                drawPath(
                    path = hintPath,
                    color = Color(0xFF7E57C2).copy(alpha = 0.5f),
                    style = Stroke(
                        width = cellWidth * 0.2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until boardSize) {
                Row(modifier = Modifier.weight(1f)) {
                    for (col in 0 until boardSize) {
                        val pos = Position(row, col)
                        val cell = gameState.board.getCell(pos)
                        val dot = gameState.dots.find {
                            it.position.row == row && it.position.col == col
                        }
                        val isSelected = gameState.selectedDot?.position == pos
                        val isHinted = gameState.currentHint?.path?.contains(pos) == true

                        CellComposable(
                            cell = cell,
                            dot = dot,
                            isSelected = isSelected,
                            isHinted = isHinted,
                            onClick = {
                                if (dot != null) {
                                    onDotSelected(dot)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CellComposable(
    cell: Cell,
    dot: Dot?,
    isSelected: Boolean,
    isHinted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSelected) 0.6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val cellColor = when {
        dot != null -> Color(dot.color.hexColor)
        cell.type == CellType.PATH && cell.color != null -> {
            val c = cell.color
            Color(c!!.hexColor).copy(alpha = 0.5f)
        }
        isHinted -> Color(0xFF7E57C2).copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isSelected -> Color.White
        cell.type == CellType.DOT -> Color.White.copy(alpha = 0.8f)
        isHinted -> Color(0xFF7E57C2)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cellColor)
            .then(
                if (isSelected) {
                    Modifier.graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                } else {
                    Modifier
                }
            )
            .border(
                width = if (isSelected || isHinted) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            },
        contentAlignment = Alignment.Center
    ) {
        if (dot != null) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .then(
                        if (isSelected) {
                            Modifier.graphicsLayer {
                                alpha = 1f - glowAlpha * 0.3f
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(dot.color.hexColor))
                )
            }
        } else if (cell.type == CellType.PATH) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun LevelsScreen(
    completedLevels: List<Int>,
    onLevelSelected: (Level) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }

            Text(
                text = "Levels",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(80.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Difficulty.entries.forEach { difficulty ->
            Text(
                text = difficulty.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            val levels = remember(difficulty) {
                (1..10).map { id ->
                    GameLogic.generateLevel(id + difficulty.ordinal * 10, difficulty)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(levels) { level ->
                    val isCompleted = level.id in completedLevels
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onLevelSelected(level) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${level.id}",
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatsScreen(
    stats: GameStats,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }

            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(80.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                StatItem("Games Played", "${stats.totalGamesPlayed}")
                StatItem("Games Won", "${stats.totalGamesWon}")
                StatItem("Win Rate", "%.1f%%".format(stats.winRate * 100))
                StatItem("Avg Time", formatTime(stats.avgTimePerGame))
                StatItem("Daily Streak", "${stats.dailyChallengeStreak}")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSettingsUpdate: (GameSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }

            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(80.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                SettingToggle(
                    title = "Dark Theme",
                    checked = settings.darkTheme,
                    onCheckedChange = { onSettingsUpdate(settings.copy(darkTheme = it)) }
                )

                SettingToggle(
                    title = "Sound Effects",
                    checked = settings.soundEnabled,
                    onCheckedChange = { onSettingsUpdate(settings.copy(soundEnabled = it)) }
                )

                SettingToggle(
                    title = "Music",
                    checked = settings.musicEnabled,
                    onCheckedChange = { onSettingsUpdate(settings.copy(musicEnabled = it)) }
                )

                SettingToggle(
                    title = "Show Tutorial",
                    checked = settings.showTutorial,
                    onCheckedChange = { onSettingsUpdate(settings.copy(showTutorial = it)) }
                )
            }
        }
    }
}

@Composable
fun SettingToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun VictoryScreen(
    moveCount: Int,
    timeMs: Long,
    levelId: Int?,
    onRestart: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
            animationSpec = tween(500),
            initialOffsetY = { it / 2 }
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Victory!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Moves",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$moveCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatTime(timeMs),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (levelId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Level $levelId completed!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRestart,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = "Continue",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
