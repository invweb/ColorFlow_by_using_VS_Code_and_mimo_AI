package com.colorflow.common.storage

expect class PlatformStorage {
    fun save(key: String, value: String)
    fun load(key: String): String?
    fun remove(key: String)
}
