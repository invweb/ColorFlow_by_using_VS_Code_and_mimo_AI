package com.colorflow.common.storage

import com.colorflow.common.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

expect class PlatformStorage {
    fun save(key: String, value: String)
    fun load(key: String): String?
    fun remove(key: String)
}

class GameStorage(private val storage: PlatformStorage) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_STATS = "game_stats"
        private const val KEY_LEVELS = "completed_levels"
        private const val KEY_DAILY = "daily_challenge"
        private const val KEY_SETTINGS = "settings"
    }

    fun saveStats(stats: GameStats) {
        storage.save(KEY_STATS, json.encodeToString(stats))
    }

    fun loadStats(): GameStats {
        val data = storage.load(KEY_STATS) ?: return GameStats()
        return try {
            json.decodeFromString<GameStats>(data)
        } catch (e: Exception) {
            GameStats()
        }
    }

    fun saveCompletedLevels(levels: List<Int>) {
        storage.save(KEY_LEVELS, json.encodeToString(levels))
    }

    fun loadCompletedLevels(): List<Int> {
        val data = storage.load(KEY_LEVELS) ?: return emptyList()
        return try {
            json.decodeFromString<List<Int>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDailyChallenge(challenge: DailyChallenge) {
        storage.save(KEY_DAILY, json.encodeToString(challenge))
    }

    fun loadDailyChallenge(): DailyChallenge? {
        val data = storage.load(KEY_DAILY) ?: return null
        return try {
            json.decodeFromString<DailyChallenge>(data)
        } catch (e: Exception) {
            null
        }
    }

    fun saveSettings(settings: GameSettings) {
        storage.save(KEY_SETTINGS, json.encodeToString(settings))
    }

    fun loadSettings(): GameSettings {
        val data = storage.load(KEY_SETTINGS) ?: return GameSettings()
        return try {
            json.decodeFromString<GameSettings>(data)
        } catch (e: Exception) {
            GameSettings()
        }
    }
}

@Serializable
data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val darkTheme: Boolean = false,
    val showTutorial: Boolean = true
)
