package com.codeoneditor.app.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Safely stages a user-selected official Bedrock APK for a future embedded runtime.
 * It never starts the installed Minecraft app and never reads/writes its world directory.
 * Staging is not equivalent to a working native runtime: the native activity/loader bridge
 * still needs to be implemented and validated against supported Minecraft versions.
 */
class RuntimeBootstrapper(private val context: Context) {
    companion object { const val OFFICIAL_PACKAGE = "com.mojang.minecraftpe" }
    private val storage = RuntimeStorage(context)

    data class ResultInfo(
        val versionName: String,
        val versionCode: Long,
        val sha256: String,
        val extractedAbis: List<String>,
        val stagedDirectory: String,
        val message: String
    )

    fun stageOfficialApk(uri: Uri): ResultInfo {
        storage.prepare().getOrThrow()
        val versionsRoot = storage.versions.canonicalFile
        val incoming = File(storage.imports, "minecraft-source-${System.currentTimeMillis()}.apk")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Não foi possível abrir o APK selecionado." }.use { source ->
                    incoming.outputStream().use { target -> source.copyTo(target) }
                }
            }
            require(incoming.length() in 1..(1_500L * 1024L * 1024L)) { "Tamanho do APK inválido ou acima de 1,5 GB." }
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageArchiveInfo(incoming.absolutePath, PackageManager.GET_META_DATA)
                ?: error("O arquivo selecionado não parece ser um APK válido.")
            require(info.packageName == OFFICIAL_PACKAGE) {
                "Pacote recusado: esperado $OFFICIAL_PACKAGE, recebido ${info.packageName}."
            }
            val versionName = info.versionName?.takeIf { it.isNotBlank() } ?: "desconhecida"
            val versionCode = getVersionCode(info)
            val hash = sha256(incoming)
            val safeVersion = "${versionCode}-${hash.take(12)}"
            val target = File(versionsRoot, safeVersion).canonicalFile
            require(target.parentFile == versionsRoot) { "Diretório de versão inválido." }
            if (target.exists()) target.deleteRecursively()
            check(target.mkdirs()) { "Não foi possível criar a pasta isolada da versão." }
            val apkCopy = File(target, "base.apk")
            incoming.copyTo(apkCopy, overwrite = true)
            val nativeRoot = File(target, "lib")
            val extracted = linkedSetOf<String>()
            ZipFile(apkCopy).use { zip ->
                val entries = zip.entries()
                var extractedBytes = 0L
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory || !entry.name.startsWith("lib/") || !entry.name.endsWith(".so")) continue
                    val parts = entry.name.split('/')
                    if (parts.size != 3 || parts[1] !in Build.SUPPORTED_ABIS) continue
                    require(entry.size <= 200L * 1024L * 1024L) { "Biblioteca nativa grande demais: ${entry.name}" }
                    extractedBytes += entry.size.coerceAtLeast(0)
                    require(extractedBytes <= 700L * 1024L * 1024L) { "Bibliotecas nativas excedem o limite de segurança." }
                    val abiDir = File(nativeRoot, parts[1])
                    check(abiDir.mkdirs() || abiDir.isDirectory) { "Não foi possível criar a pasta da ABI." }
                    val output = File(abiDir, parts[2]).canonicalFile
                    require(output.parentFile == abiDir.canonicalFile) { "Caminho de biblioteca inválido." }
                    zip.getInputStream(entry).use { source -> output.outputStream().use { dest -> source.copyTo(dest) } }
                    extracted += parts[1]
                }
            }
            require(extracted.isNotEmpty()) { "O APK não contém bibliotecas nativas para as arquiteturas deste dispositivo (${Build.SUPPORTED_ABIS.joinToString()})." }
            val manifest = JSONObject().apply {
                put("packageName", info.packageName)
                put("versionName", versionName)
                put("versionCode", versionCode)
                put("sha256", hash)
                put("stagedAt", System.currentTimeMillis())
                put("abis", org.json.JSONArray(extracted.toList()))
                put("nativeRuntimeReady", false)
                put("worldsCopied", false)
            }
            File(target, "staged-runtime.json").writeText(manifest.toString(2))
            return ResultInfo(
                versionName, versionCode, hash, extracted.toList(), target.absolutePath,
                "APK validado e preparado em área privada. O carregador nativo ainda não está integrado; o jogo não foi iniciado."
            )
        } catch (t: Throwable) {
            throw t
        } finally {
            incoming.delete()
        }
    }

    @Suppress("DEPRECATION")
    private fun getVersionCode(info: android.content.pm.PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
