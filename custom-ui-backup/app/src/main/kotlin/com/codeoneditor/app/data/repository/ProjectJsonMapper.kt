package com.codeoneditor.app.data.repository

import com.codeoneditor.app.data.model.FileItem
import com.codeoneditor.core.editor.model.FileType
import com.codeoneditor.app.data.model.ProjectItem
import org.json.JSONArray
import org.json.JSONObject

/** JSON persistence for project metadata and the lightweight in-memory file tree. */
object ProjectJsonMapper {
    fun projectToJson(project: ProjectItem): JSONObject = JSONObject().apply {
        put("id", project.id)
        put("name", project.name)
        put("packageName", project.packageName)
        put("createdAt", project.createdAt)
        put("language", project.language)
        put("minSdk", project.minSdk)
        put("sourceUri", project.sourceUri)
        put("rootPath", project.rootPath)
        put("files", JSONArray().apply {
            project.files.forEach { put(fileToJson(it)) }
        })
    }

    fun projectFromJson(json: JSONObject): ProjectItem {
        val files = mutableListOf<FileItem>()
        val filesJson = json.optJSONArray("files")
        if (filesJson != null) {
            for (i in 0 until filesJson.length()) {
                files.add(fileFromJson(filesJson.optJSONObject(i) ?: continue))
            }
        }
        return ProjectItem(
            id = json.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            name = json.optString("name", "Projeto"),
            packageName = json.optString("packageName", "com.codeoneditor.app.projeto"),
            createdAt = json.optString("createdAt", ""),
            language = json.optString("language", "Kotlin"),
            minSdk = json.optInt("minSdk", 24),
            files = files,
            sourceUri = json.optString("sourceUri").takeIf { it.isNotBlank() },
            rootPath = json.optString("rootPath").takeIf { it.isNotBlank() }
        )
    }

    private fun fileToJson(file: FileItem): JSONObject = JSONObject().apply {
        put("id", file.id)
        put("name", file.name)
        put("path", file.path)
        put("isDirectory", file.isDirectory)
        // File contents are transient and can be large. Open files are persisted separately.
        if (file.content.isNotEmpty()) put("content", file.content)
        put("isExpanded", file.isExpanded)
        put("fileType", file.fileType.name)
        put("sourceUri", file.sourceUri)
        put("childrenLoaded", file.childrenLoaded)
        put("children", JSONArray().apply {
            file.children.forEach { put(fileToJson(it)) }
        })
    }

    private fun fileFromJson(json: JSONObject): FileItem {
        val children = mutableListOf<FileItem>()
        val childrenJson = json.optJSONArray("children")
        if (childrenJson != null) {
            for (i in 0 until childrenJson.length()) {
                children.add(fileFromJson(childrenJson.optJSONObject(i) ?: continue))
            }
        }
        return FileItem(
            id = json.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            name = json.optString("name", ""),
            path = json.optString("path", json.optString("name", "")),
            isDirectory = json.optBoolean("isDirectory", false),
            content = json.optString("content", ""),
            children = children,
            isExpanded = json.optBoolean("isExpanded", false),
            fileType = runCatching { FileType.valueOf(json.optString("fileType")) }.getOrDefault(FileType.OTHER),
            sourceUri = json.optString("sourceUri").takeIf { it.isNotBlank() },
            childrenLoaded = json.optBoolean("childrenLoaded", !json.optBoolean("isDirectory", false))
        )
    }
}
