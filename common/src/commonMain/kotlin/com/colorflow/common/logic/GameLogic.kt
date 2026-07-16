package com.colorflow.common.logic

import com.colorflow.common.model.*
import kotlin.random.Random

object GameLogic {

    private val DIRECTIONS = listOf(
        Position(-1, 0), Position(1, 0), Position(0, -1), Position(0, 1)
    )

    fun findPath(board: Board, start: Position, end: Position): List<Position>? {
        if (!board.isInBounds(start) || !board.isInBounds(end)) return null
        if (start == end) return listOf(start)

        val queue = ArrayDeque<List<Position>>()
        val visited = mutableSetOf<Position>()
        queue.add(listOf(start))
        visited.add(start)

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val current = path.last()

            for (dir in DIRECTIONS) {
                val next = Position(current.row + dir.row, current.col + dir.col)
                if (!board.isInBounds(next)) continue
                if (next in visited) continue
                if (next != end && board.isOccupied(next)) continue

                val newPath = path + next
                if (next == end) return newPath

                visited.add(next)
                queue.add(newPath)
            }
        }
        return null
    }

    fun isValidConnection(
        board: Board,
        path: List<Position>,
        connections: List<Connection>,
        excludePairId: Int
    ): Boolean {
        if (path.size < 2) return false

        for (pos in path) {
            if (!board.isInBounds(pos)) return false
        }

        val otherPaths = connections
            .filter { it.isComplete && it.dot1.pairId != excludePairId }
            .flatMap { it.path }

        for (pos in path) {
            if (pos in otherPaths) return false
        }

        return true
    }

    fun connectDots(state: GameState, from: Dot, to: Dot): GameState? {
        if (from.pairId != to.pairId) return null
        if (from.position == to.position) return null

        val path = findPath(state.board, from.position, to.position) ?: return null

        if (!isValidConnection(state.board, path, state.connections, from.pairId)) return null

        val newConnections = state.connections.map { conn ->
            if (conn.dot1.pairId == from.pairId) {
                conn.copy(path = path, isComplete = true)
            } else {
                conn
            }
        }

        val newBoard = applyPathToBoard(state.board, path, from.color, from.pairId)
        val newMoveCount = state.moveCount + 1
        val isWon = newConnections.all { it.isComplete }

        return state.copy(
            board = newBoard,
            connections = newConnections,
            selectedDot = null,
            moveCount = newMoveCount,
            isWon = isWon,
            currentHint = null
        )
    }

    private fun applyPathToBoard(
        board: Board,
        path: List<Position>,
        color: CellColor,
        pairId: Int
    ): Board {
        val newCells = board.cells.map { row -> row.copyOf() }.toTypedArray()

        for ((index, pos) in path.withIndex()) {
            val cellType = if (index == 0 || index == path.lastIndex) CellType.DOT else CellType.PATH
            newCells[pos.row][pos.col] = Cell(
                type = cellType,
                color = color,
                pairId = pairId,
                pathIndex = index
            )
        }

        return Board(newCells, board.size)
    }

    fun removeConnection(state: GameState, pairId: Int): GameState {
        val conn = state.connections.find { it.dot1.pairId == pairId } ?: return state

        val newBoard = clearPathFromBoard(state.board, conn.path)
        val newConnections = state.connections.map {
            if (it.dot1.pairId == pairId) {
                it.copy(path = emptyList(), isComplete = false)
            } else {
                it
            }
        }

        return state.copy(
            board = newBoard,
            connections = newConnections
        )
    }

    private fun clearPathFromBoard(board: Board, path: List<Position>): Board {
        val newCells = board.cells.map { row -> row.copyOf() }.toTypedArray()

        for ((index, pos) in path.withIndex()) {
            if (index == 0 || index == path.lastIndex) {
                val oldCell = board.getCell(pos)
                newCells[pos.row][pos.col] = Cell(
                    type = CellType.DOT,
                    color = oldCell.color,
                    pairId = oldCell.pairId,
                    pathIndex = -1
                )
            } else {
                newCells[pos.row][pos.col] = Cell()
            }
        }

        return Board(newCells, board.size)
    }

    fun saveSnapshot(state: GameState): GameState {
        val snapshot = MoveSnapshot(
            board = state.board,
            connections = state.connections,
            selectedDot = state.selectedDot,
            moveCount = state.moveCount
        )
        return state.copy(undoStack = state.undoStack + snapshot)
    }

    fun undoMove(state: GameState): GameState {
        if (state.undoStack.isEmpty()) return state

        val snapshot = state.undoStack.last()
        return state.copy(
            board = snapshot.board,
            connections = snapshot.connections,
            selectedDot = null,
            moveCount = snapshot.moveCount,
            undoStack = state.undoStack.dropLast(1),
            isWon = false,
            currentHint = null
        )
    }

    fun isBoardFull(board: Board): Boolean {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.cells[row][col].type == CellType.EMPTY) return false
            }
        }
        return true
    }

    fun checkWin(state: GameState): Boolean {
        return state.connections.all { it.isComplete }
    }

    fun generateBoard(size: Int = 6, numPairs: Int = 6): Pair<Board, List<Dot>> {
        val colors = CellColor.entries.take(numPairs)
        val maxAttempts = 10000

        repeat(maxAttempts) {
            val result = tryGenerateBoard(size, colors)
            if (result != null) return result
        }
        error("Failed to generate board after $maxAttempts attempts")
    }

    private fun tryGenerateBoard(size: Int, colors: List<CellColor>): Pair<Board, List<Dot>>? {
        val cells = Array(size) { Array(size) { Cell() } }
        val dots = mutableListOf<Dot>()
        val usedPositions = mutableSetOf<Position>()

        for ((pairId, color) in colors.withIndex()) {
            val validPositions = mutableListOf<Position>()
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val pos = Position(row, col)
                    if (pos !in usedPositions) {
                        validPositions.add(pos)
                    }
                }
            }

            if (validPositions.size < 2) return null

            val shuffled = validPositions.shuffled()
            val pos1 = shuffled[0]
            val pos2 = shuffled.drop(1).firstOrNull { candidate ->
                val path = findPathForGeneration(cells, pos1, candidate, size)
                path != null
            } ?: return null

            val path = findPathForGeneration(cells, pos1, pos2, size) ?: return null

            for ((index, pos) in path.withIndex()) {
                val cellType = if (index == 0 || index == path.lastIndex) CellType.DOT else CellType.PATH
                cells[pos.row][pos.col] = Cell(
                    type = cellType,
                    color = color,
                    pairId = pairId,
                    pathIndex = index
                )
                usedPositions.add(pos)
            }

            dots.add(Dot(pos1, color, pairId))
            dots.add(Dot(pos2, color, pairId))
        }

        val board = Board(cells, size)

        val clearedCells = Array(size) { row ->
            Array(size) { col ->
                val cell = cells[row][col]
                if (cell.type == CellType.PATH) {
                    Cell()
                } else {
                    cell
                }
            }
        }

        return Pair(Board(clearedCells, size), dots)
    }

    private fun findPathForGeneration(
        cells: Array<Array<Cell>>,
        start: Position,
        end: Position,
        size: Int
    ): List<Position>? {
        if (start == end) return listOf(start)

        val queue = ArrayDeque<List<Position>>()
        val visited = mutableSetOf<Position>()
        queue.add(listOf(start))
        visited.add(start)

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val current = path.last()

            for (dir in DIRECTIONS) {
                val next = Position(current.row + dir.row, current.col + dir.col)
                if (next.row !in 0 until size || next.col !in 0 until size) continue
                if (next in visited) continue
                if (next != end && cells[next.row][next.col].type != CellType.EMPTY) continue

                val newPath = path + next
                if (next == end) return newPath

                visited.add(next)
                queue.add(newPath)
            }
        }
        return null
    }

    fun useHint(state: GameState): GameState {
        if (state.hintsRemaining <= 0) return state

        val incompleteConns = state.connections.filter { !it.isComplete }
        if (incompleteConns.isEmpty()) return state

        val conn = incompleteConns.random()
        val tempBoard = clearCompletedPaths(state.board, state.connections.filter { it.isComplete && it.dot1.pairId != conn.dot1.pairId })
        val path = findPath(tempBoard, conn.dot1.position, conn.dot2.position)

        if (path == null) return state

        return state.copy(
            hintsRemaining = state.hintsRemaining - 1,
            currentHint = Hint(conn.dot1, conn.dot2, path)
        )
    }

    private fun clearCompletedPaths(board: Board, completedConnections: List<Connection>): Board {
        val newCells = board.cells.map { row -> row.copyOf() }.toTypedArray()

        for (conn in completedConnections) {
            for ((index, pos) in conn.path.withIndex()) {
                if (index != 0 && index != conn.path.lastIndex) {
                    newCells[pos.row][pos.col] = Cell()
                }
            }
        }

        return Board(newCells, board.size)
    }

    fun generateLevel(id: Int, difficulty: Difficulty): Level {
        val (board, dots) = generateBoard(difficulty.boardSize, difficulty.numPairs)
        return Level(
            id = id,
            difficulty = difficulty,
            board = board,
            dots = dots
        )
    }

    fun generateDailyChallenge(date: String): DailyChallenge {
        val seed = date.hashCode().toLong()
        val random = Random(seed)

        val difficulty = when (random.nextInt(3)) {
            0 -> Difficulty.EASY
            1 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }

        val level = generateLevel(0, difficulty)
        return DailyChallenge(
            date = date,
            level = level
        )
    }

    fun createTestBoard(): Pair<Board, List<Dot>> {
        val size = 6
        val cells = Array(size) { Array(size) { Cell() } }

        val pairs = listOf(
            (0 to ColorPair(Position(0, 0), Position(2, 0), CellColor.RED)),
            (1 to ColorPair(Position(0, 1), Position(0, 5), CellColor.BLUE)),
            (2 to ColorPair(Position(1, 0), Position(5, 0), CellColor.GREEN)),
            (3 to ColorPair(Position(3, 3), Position(5, 5), CellColor.YELLOW)),
            (4 to ColorPair(Position(1, 5), Position(5, 3), CellColor.PURPLE)),
            (5 to ColorPair(Position(3, 0), Position(5, 2), CellColor.ORANGE))
        )

        val dots = mutableListOf<Dot>()

        for ((pairId, pair) in pairs) {
            val (p1, p2, color) = pair
            cells[p1.row][p1.col] = Cell(CellType.DOT, color, pairId)
            cells[p2.row][p2.col] = Cell(CellType.DOT, color, pairId)
            dots.add(Dot(p1, color, pairId))
            dots.add(Dot(p2, color, pairId))
        }

        return Pair(Board(cells, size), dots)
    }

    private data class ColorPair(
        val pos1: Position,
        val pos2: Position,
        val color: CellColor
    )
}
