package com.codeoneditor.app.runtime

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Creates a clean, app-private data root for each launcher profile. It never resolves or
 * writes into com.mojang.minecraftpe's private data. This is a storage boundary, not proof
 * that Minecraft itself is using the boundary; only the embedded loader can guarantee that.
 */
class RuntimeIsolationManager(context: Context) {
    private val storage = RuntimeStorage(context.applicationContext)
    private val profileRoot = File(storage.profiles, "isolated-data")

    data class IsolatedProfile(val id: String, val name: String, val root: File, val createdAt: Long)

    @Synchronized
    fun createProfile(name: String): IsolatedProfile {
        storage.prepare().getOrThrow()
        val cleanName = name.trim().replace(Regex("\\s+"), " ").take(40)
        require(cleanName.isNotBlank()) { "Informe um nome para o perfil." }
        val root = profileRoot.canonicalFile
        check(root.path.startsWith(storage.profiles.canonicalPath + File.separator)) {
            "A pasta de perfil saiu do armazenamento privado esperado."
        }
        check(root.mkdirs() || root.isDirectory) { "Não foi possível criar a raiz isolada." }
        val id = UUID.randomUUID().toString()
        val dir = File(root, id).canonicalFile
        require(dir.parentFile == root) { "Caminho de perfil inválido." }
        check(dir.mkdirs()) { "Não foi possível criar o perfil isolado." }
        listOf("files", "data", "cache", "worlds", "resource_packs", "behavior_packs", "mods", "logs", "backups").forEach {
            val child = File(dir, it)
            check(child.mkdirs() || child.isDirectory) { "Não foi possível criar a pasta $it." }
        }
        val createdAt = System.currentTimeMillis()
        val manifest = JSONObject().apply {
            put("schemaVersion", 1)
            put("profileId", id)
            put("name", cleanName)
            put("createdAt", createdAt)
            put("storageMode", "launcher-private-empty")
            put("originalWorldsCopied", false)
            put("nativeLoaderVerified", false)
        }
        File(dir, "profile.json").writeText(manifest.toString(2))
        return IsolatedProfile(id, cleanName, dir, createdAt)
    }

    fun validate(profile: IsolatedProfile): Result<String> = runCatching {
        val root = profileRoot.canonicalFile
        val dir = profile.root.canonicalFile
        require(dir.path.startsWith(root.path + File.separator)) { "Perfil fora da pasta isolada." }
        require(File(dir, "profile.json").isFile) { "Manifesto do perfil ausente." }
        val json = JSONObject(File(dir, "profile.json").readText())
        require(json.optString("profileId") == profile.id) { "O ID do manifesto não corresponde ao perfil." }
        require(!json.optBoolean("originalWorldsCopied", true)) { "Perfil não é vazio; revisão manual necessária." }
        require(listOf("files", "data", "cache", "worlds", "resource_packs", "behavior_packs", "mods", "logs", "backups").all { File(dir, it).isDirectory }) {
            "Estrutura do perfil incompleta."
        }
        "Perfil isolado criado e validado no armazenamento privado. O carregador ainda precisa redirecionar o jogo para estas pastas."
    }
}
