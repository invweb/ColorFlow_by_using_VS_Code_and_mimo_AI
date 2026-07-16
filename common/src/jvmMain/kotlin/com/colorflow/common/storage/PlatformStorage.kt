package com.colorflow.common.storage

import java.io.File
import java.util.Properties

actual class PlatformStorage {
    private val prefsFile = File(System.getProperty("user.home"), ".colorflow/config.properties")
    private val props = Properties()

    init {
        if (prefsFile.exists()) {
            prefsFile.inputStream().use { props.load(it) }
        }
    }

    actual fun save(key: String, value: String) {
        props.setProperty(key, value)
        prefsFile.parentFile?.mkdirs()
        prefsFile.outputStream().use { props.store(it, null) }
    }

    actual fun load(key: String): String? {
        return props.getProperty(key)
    }

    actual fun remove(key: String) {
        props.remove(key)
        prefsFile.outputStream().use { props.store(it, null) }
    }
}
