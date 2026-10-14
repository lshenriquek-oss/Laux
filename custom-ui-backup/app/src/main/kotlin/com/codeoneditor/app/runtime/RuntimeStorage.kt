package com.codeoneditor.app.runtime

import android.content.Context
import java.io.File

/**
 * Prepares private, launcher-owned directories for the future runtime integration.
 * This does not copy, patch, or launch Minecraft and does not touch game worlds.
 */
class RuntimeStorage(context: Context) {
    private val root = File(context.filesDir, "bedrock-runtime")

    val versions = directory("versions")
    val profiles = directory("profiles")
    val libraries = directory("runtime-libs")
    val mods = directory("mods")
    val backups = directory("backups")
    val logs = directory("logs")
    val imports = directory("imports")

    private fun directory(name: String): File = File(root, name)

    fun prepare(): Result<StorageStatus> = runCatching {
        val directories = listOf(versions, profiles, libraries, mods, backups, logs, imports)
        directories.forEach { dir ->
            if (!dir.exists() && !dir.mkdirs()) {
                error("Não foi possível criar ${dir.name}.")
            }
            check(dir.isDirectory && dir.canWrite()) { "Pasta indisponível para escrita: ${dir.name}." }
        }
        StorageStatus(
            rootPath = root.absolutePath,
            directoryCount = directories.size,
            writable = directories.all { it.isDirectory && it.canWrite() }
        )
    }

    fun describe(): List<Pair<String, File>> = listOf(
        "Versões" to versions,
        "Perfis" to profiles,
        "Bibliotecas do runtime" to libraries,
        "Mods" to mods,
        "Backups" to backups,
        "Logs" to logs,
        "Importações" to imports
    )
}

data class StorageStatus(
    val rootPath: String,
    val directoryCount: Int,
    val writable: Boolean
)
