package com.colorflow.common.logic

import com.colorflow.common.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameLogicTest {

    @Test
    fun testPathThroughOccupiedCellIsRejected() {
        val size = 5
        val cells = Array(size) { row ->
            Array(size) { col ->
                when {
                    row == 0 && col == 0 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 0 && col == 4 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 2 && col == 1 -> Cell(CellType.PATH, CellColor.BLUE, 1)
                    row == 2 && col == 2 -> Cell(CellType.PATH, CellColor.BLUE, 1)
                    row == 2 && col == 3 -> Cell(CellType.DOT, CellColor.BLUE, 1)
                    row == 4 && col == 0 -> Cell(CellType.DOT, CellColor.BLUE, 1)
                    else -> Cell()
                }
            }
        }

        val board = Board(cells, size)

        val manualPath = listOf(
            Position(0, 0), Position(1, 0), Position(2, 0),
            Position(2, 1), Position(2, 2), Position(2, 3),
            Position(1, 3), Position(0, 3), Position(0, 4)
        )

        val connections = listOf(
            Connection(
                Dot(Position(4, 0), CellColor.BLUE, 1),
                Dot(Position(2, 3), CellColor.BLUE, 1),
                listOf(Position(4, 0), Position(2, 3)),
                true
            )
        )

        val isValid = GameLogic.isValidConnection(board, manualPath, connections, 0)
        assertFalse(isValid, "Path crossing occupied cells should be rejected")
    }

    @Test
    fun testCannotConnectDifferentColors() {
        val size = 3
        val cells = Array(size) { row ->
            Array(size) { col ->
                when {
                    row == 0 && col == 0 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 2 && col == 2 -> Cell(CellType.DOT, CellColor.BLUE, 1)
                    else -> Cell()
                }
            }
        }

        val board = Board(cells, size)
        val dot1 = Dot(Position(0, 0), CellColor.RED, 0)
        val dot2 = Dot(Position(2, 2), CellColor.BLUE, 1)

        val state = GameState.create(
            dots = listOf(dot1, dot2),
            board = board
        )

        val result = GameLogic.connectDots(state, dot1, dot2)
        assertNull(result, "Cannot connect dots of different colors")
    }

    @Test
    fun testWinDetectionWhenAllPairsConnected() {
        val size = 2
        val cells = Array(size) { row ->
            Array(size) { col ->
                when {
                    row == 0 && col == 0 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 0 && col == 1 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 1 && col == 0 -> Cell(CellType.DOT, CellColor.BLUE, 1)
                    row == 1 && col == 1 -> Cell(CellType.DOT, CellColor.BLUE, 1)
                    else -> Cell()
                }
            }
        }

        val board = Board(cells, size)
        val dots = listOf(
            Dot(Position(0, 0), CellColor.RED, 0),
            Dot(Position(0, 1), CellColor.RED, 0),
            Dot(Position(1, 0), CellColor.BLUE, 1),
            Dot(Position(1, 1), CellColor.BLUE, 1)
        )

        var state = GameState.create(dots, board)

        val saved1 = GameLogic.saveSnapshot(state)
        state = GameLogic.connectDots(saved1, dots[0], dots[1])!!

        val saved2 = GameLogic.saveSnapshot(state)
        state = GameLogic.connectDots(saved2, dots[2], dots[3])!!

        assertTrue(GameLogic.checkWin(state), "Game should be won when all pairs connected and board full")
    }

    @Test
    fun testUndoRestoresPreviousState() {
        val size = 3
        val cells = Array(size) { row ->
            Array(size) { col ->
                when {
                    row == 0 && col == 0 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 0 && col == 2 -> Cell(CellType.DOT, CellColor.RED, 0)
                    else -> Cell()
                }
            }
        }

        val board = Board(cells, size)
        val dots = listOf(
            Dot(Position(0, 0), CellColor.RED, 0),
            Dot(Position(0, 2), CellColor.RED, 0)
        )

        var state = GameState.create(dots, board)

        val initialMoveCount = state.moveCount
        val initialBoardHash = state.board.hashCode()

        val saved1 = GameLogic.saveSnapshot(state)
        state = GameLogic.connectDots(saved1, dots[0], dots[1])!!

        assertEquals(initialMoveCount + 1, state.moveCount)

        state = GameLogic.undoMove(state)

        assertEquals(initialMoveCount, state.moveCount)
        assertEquals(initialBoardHash, state.board.hashCode())
        assertFalse(state.isWon)
    }

    @Test
    fun testGeneratedBoardHasValidDotPairs() {
        val (board, dots) = GameLogic.generateBoard(size = 6, numPairs = 6)

        assertEquals(12, dots.size, "Should have 12 dots (6 pairs)")

        val pairIds = dots.map { it.pairId }.toSet()
        assertEquals(6, pairIds.size, "Should have 6 unique pair IDs")

        for (pairId in pairIds) {
            val pairDots = dots.filter { it.pairId == pairId }
            assertEquals(2, pairDots.size, "Each pair should have exactly 2 dots")
            assertEquals(pairDots[0].color, pairDots[1].color, "Pair dots should have same color")
        }

        val allDotsInBounds = dots.all { board.isInBounds(it.position) }
        assertTrue(allDotsInBounds, "All dots should be within board bounds")

        for (dot in dots) {
            val cell = board.getCell(dot.position)
            assertEquals(CellType.DOT, cell.type, "Dot positions should have DOT cell type")
            assertEquals(dot.color, cell.color)
        }
    }

    @Test
    fun testFindPathReturnsCorrectPath() {
        val size = 3
        val cells = Array(size) { Array(size) { Cell() } }
        cells[0][0] = Cell(CellType.DOT, CellColor.RED, 0)
        cells[2][2] = Cell(CellType.DOT, CellColor.RED, 0)

        val board = Board(cells, size)
        val path = GameLogic.findPath(board, Position(0, 0), Position(2, 2))

        assertNotNull(path)
        assertEquals(Position(0, 0), path.first())
        assertEquals(Position(2, 2), path.last())
        assertTrue(path.size >= 3, "Path should have at least 3 cells")
    }

    @Test
    fun testUndoWithEmptyStackReturnsSameState() {
        val size = 3
        val cells = Array(size) { row ->
            Array(size) { col ->
                when {
                    row == 0 && col == 0 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 0 && col == 2 -> Cell(CellType.DOT, CellColor.RED, 0)
                    else -> Cell()
                }
            }
        }

        val board = Board(cells, size)
        val dots = listOf(
            Dot(Position(0, 0), CellColor.RED, 0),
            Dot(Position(0, 2), CellColor.RED, 0)
        )
        val state = GameState.create(dots, board)

        val result = GameLogic.undoMove(state)
        assertEquals(state, result, "Undo on empty stack should return same state")
    }

    @Test
    fun testBoardIsFullAfterAllConnections() {
        val size = 2
        val cells = Array(size) { row ->
            Array(size) { col ->
                when {
                    row == 0 && col == 0 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 0 && col == 1 -> Cell(CellType.DOT, CellColor.RED, 0)
                    row == 1 && col == 0 -> Cell(CellType.DOT, CellColor.BLUE, 1)
                    row == 1 && col == 1 -> Cell(CellType.DOT, CellColor.BLUE, 1)
                    else -> Cell()
                }
            }
        }

        val board = Board(cells, size)
        assertTrue(GameLogic.isBoardFull(board), "Board with all cells occupied should be full")
    }
}
