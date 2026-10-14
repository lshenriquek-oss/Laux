package com.codeoneditor.app.runtime

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Local profile registry for the upcoming runtime integration.
 * Profiles are metadata/storage boundaries only; this class does not launch or isolate Minecraft.
 */
class RuntimeProfileManager(context: Context) {
    private val storage = RuntimeStorage(context)
    private val registry = File(storage.profiles, "profiles.json")

    data class Profile(
        val id: String,
        val name: String,
        val createdAt: Long,
        val gameDataDirectory: String,
        val enabled: Boolean = true
    )

    @Synchronized
    fun list(): List<Profile> {
        storage.prepare().getOrThrow()
        if (!registry.exists()) return emptyList()
        val root = JSONObject(registry.readText())
        val profiles = root.optJSONArray("profiles") ?: return emptyList()
        return buildList {
            for (i in 0 until profiles.length()) {
                val item = profiles.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (!isSafeId(id)) continue
                add(Profile(
                    id = id,
                    name = item.optString("name", "Perfil"),
                    createdAt = item.optLong("createdAt", 0L),
                    gameDataDirectory = profileDirectory(id).absolutePath,
                    enabled = item.optBoolean("enabled", true)
                ))
            }
        }
    }

    @Synchronized
    fun create(name: String): Profile {
        storage.prepare().getOrThrow()
        val cleanName = name.trim().replace(Regex("\\s+"), " ").take(40)
        require(cleanName.isNotBlank()) { "Informe um nome para o perfil." }
        val existing = list()
        require(existing.none { it.name.equals(cleanName, ignoreCase = true) }) {
            "Já existe um perfil com esse nome."
        }
        val id = UUID.randomUUID().toString()
        val dataDirectory = profileDirectory(id)
        check(dataDirectory.mkdirs() || dataDirectory.isDirectory) {
            "Não foi possível criar a pasta de dados do perfil."
        }
        listOf("worlds", "config", "logs", "mods").forEach { child ->
            val dir = File(dataDirectory, child)
            check(dir.mkdirs() || dir.isDirectory) { "Não foi possível preparar a pasta $child." }
        }
        val created = Profile(id, cleanName, System.currentTimeMillis(), dataDirectory.absolutePath)
        writeProfiles(existing + created)
        return created
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean) {
        require(isSafeId(id)) { "Identificador de perfil inválido." }
        val profiles = list()
        require(profiles.any { it.id == id }) { "Perfil não encontrado." }
        writeProfiles(profiles.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    private fun writeProfiles(profiles: List<Profile>) {
        val root = JSONObject()
        val array = org.json.JSONArray()
        profiles.forEach { profile ->
            array.put(JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("createdAt", profile.createdAt)
                put("enabled", profile.enabled)
            })
        }
        root.put("schemaVersion", 1)
        root.put("profiles", array)
        val temp = File(registry.parentFile, "profiles.json.tmp")
        temp.writeText(root.toString(2))
        if (registry.exists() && !registry.delete()) error("Não foi possível atualizar o registro de perfis.")
        check(temp.renameTo(registry)) { "Não foi possível salvar o registro de perfis." }
    }

    private fun profileDirectory(id: String): File {
        require(isSafeId(id)) { "Identificador de perfil inválido." }
        val base = File(storage.profiles, "instances").canonicalFile
        val target = File(base, id).canonicalFile
        require(target.parentFile == base) { "Caminho de perfil inválido." }
        return target
    }

    private fun isSafeId(id: String): Boolean =
        id.matches(Regex("[a-fA-F0-9-]{36}")) && !id.contains("..")
}
