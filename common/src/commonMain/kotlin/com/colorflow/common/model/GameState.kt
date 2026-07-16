package com.colorflow.common.model

import kotlinx.serialization.Serializable

@Serializable
data class Position(val row: Int, val col: Int)

@Serializable
enum class CellColor(val hexColor: Long) {
    RED(0xFFE53935L),
    BLUE(0xFF1E88E5L),
    GREEN(0xFF43A047L),
    YELLOW(0xFFFDD835L),
    PURPLE(0xFF8E24AAL),
    ORANGE(0xFFFF8F00L)
}

@Serializable
data class Dot(
    val position: Position,
    val color: CellColor,
    val pairId: Int
)

@Serializable
enum class CellType {
    EMPTY,
    DOT,
    PATH
}

@Serializable
data class Cell(
    val type: CellType = CellType.EMPTY,
    val color: CellColor? = null,
    val pairId: Int = -1,
    val pathIndex: Int = -1
)

@Serializable
data class Board(
    val cells: Array<Array<Cell>>,
    val size: Int = 6
) {
    fun getCell(pos: Position): Cell = cells[pos.row][pos.col]

    fun isInBounds(pos: Position): Boolean =
        pos.row in 0 until size && pos.col in 0 until size

    fun isOccupied(pos: Position): Boolean =
        !isInBounds(pos) || getCell(pos).type != CellType.EMPTY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false
        return size == other.size && cells.contentDeepEquals(other.cells)
    }

    override fun hashCode(): Int = cells.contentDeepHashCode()
}

@Serializable
data class Connection(
    val dot1: Dot,
    val dot2: Dot,
    val path: List<Position> = emptyList(),
    val isComplete: Boolean = false
)

@Serializable
data class MoveSnapshot(
    val board: Board,
    val connections: List<Connection>,
    val selectedDot: Dot?,
    val moveCount: Int
)

@Serializable
enum class Difficulty(val boardSize: Int, val numPairs: Int) {
    EASY(6, 4),
    MEDIUM(8, 6),
    HARD(10, 8)
}

@Serializable
data class Level(
    val id: Int,
    val difficulty: Difficulty,
    val board: Board,
    val dots: List<Dot>,
    val bestMoves: Int = -1,
    val bestTimeMs: Long = -1,
    val isCompleted: Boolean = false
)

@Serializable
data class DailyChallenge(
    val date: String,
    val level: Level,
    val isCompleted: Boolean = false,
    val bestMoves: Int = -1,
    val bestTimeMs: Long = -1
)

@Serializable
data class GameStats(
    val totalGamesPlayed: Int = 0,
    val totalGamesWon: Int = 0,
    val totalTimeMs: Long = 0,
    val bestMovesByDifficulty: Map<Difficulty, Int> = emptyMap(),
    val bestTimeByDifficulty: Map<Difficulty, Long> = emptyMap(),
    val dailyChallengeStreak: Int = 0,
    val lastDailyChallengeDate: String? = null
) {
    val winRate: Float
        get() = if (totalGamesPlayed > 0) totalGamesWon.toFloat() / totalGamesPlayed else 0f

    val avgTimePerGame: Long
        get() = if (totalGamesWon > 0) totalTimeMs / totalGamesWon else 0
}

@Serializable
data class Hint(
    val fromDot: Dot,
    val toDot: Dot,
    val path: List<Position>
)

@Serializable
data class GameState(
    val board: Board,
    val dots: List<Dot>,
    val connections: List<Connection>,
    val selectedDot: Dot? = null,
    val moveCount: Int = 0,
    val undoStack: List<MoveSnapshot> = emptyList(),
    val isWon: Boolean = false,
    val currentLevel: Level? = null,
    val hintsRemaining: Int = 3,
    val currentHint: Hint? = null,
    val elapsedTimeMs: Long = 0,
    val isTimerRunning: Boolean = false
) {
    companion object {
        fun create(dots: List<Dot>, board: Board, level: Level? = null): GameState {
            return GameState(
                board = board,
                dots = dots,
                connections = dots.chunked(2).mapIndexed { index, pair ->
                    Connection(
                        dot1 = pair[0],
                        dot2 = pair[1],
                        isComplete = false
                    )
                },
                currentLevel = level
            )
        }
    }

    fun getCell(pos: Position): Cell = board.getCell(pos)
    fun isInBounds(pos: Position): Boolean = board.isInBounds(pos)

    fun getIncompleteConnections(): List<Connection> =
        connections.filter { !it.isComplete }

    fun getCompleteConnections(): List<Connection> =
        connections.filter { it.isComplete }

    fun getDotsForConnection(conn: Connection): List<Dot> =
        listOf(conn.dot1, conn.dot2)

    val progress: Float
        get() {
            val total = connections.size
            if (total == 0) return 0f
            return getCompleteConnections().size.toFloat() / total
        }
}
