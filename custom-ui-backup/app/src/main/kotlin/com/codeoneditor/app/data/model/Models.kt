package com.codeoneditor.app.data.model

import com.codeoneditor.core.editor.model.FileType
import java.util.UUID

data class ProjectItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packageName: String = "com.codeoneditor.app.${name.lowercase().replace(" ", "")}",
    val createdAt: String = "",
    val language: String = "",
    val minSdk: Int = 26,
    val files: MutableList<FileItem> = mutableListOf(),
    /** SAF tree URI for projects opened directly from external storage. */
    val sourceUri: String? = null,
    /** Absolute root for app-managed projects, persisted so reopening never points at a stale/default workspace. */
    val rootPath: String? = null
)

data class FileItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val isDirectory: Boolean = false,
    /** File contents are intentionally transient in the project tree. Open files are loaded on demand into EditorTab. */
    val content: String = "",
    val children: MutableList<FileItem> = mutableListOf(),
    val isExpanded: Boolean = false,
    val fileType: FileType = FileType.OTHER,
    /** URI of the real filesystem document when the project is SAF-backed. */
    val sourceUri: String? = null,
    /** Directories are populated only when expanded. */
    val childrenLoaded: Boolean = !isDirectory
)

data class AppSettingsState(
    val themeMode: String = "Automático / Sistema",
    val appLanguage: String = "Português",
    val autoSave: Boolean = true,
    val openLastProjectOnStart: Boolean = false,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val showWhitespace: Boolean = false,
    val autoCloseBrackets: Boolean = true,
    val pinLineNumbers: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val ligatures: Boolean = true,
)
