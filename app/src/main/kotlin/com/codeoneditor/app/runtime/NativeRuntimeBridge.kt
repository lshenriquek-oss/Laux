package com.codeoneditor.app.runtime

import android.os.Build
import java.io.File

/** Preflight only. It intentionally never reports that Minecraft is launchable. */
object NativeRuntimeBridge {
    data class Preflight(val available: Boolean, val readyToLaunch: Boolean, val message: String)

    private val loadFailure: String? = try {
        System.loadLibrary("bedrock_runtime_bridge")
        null
    } catch (error: UnsatisfiedLinkError) {
        error.message ?: "Biblioteca JNI indisponível"
    } catch (error: SecurityException) {
        error.message ?: "Carregamento JNI bloqueado"
    }

    fun inspect(stagedDirectory: String?): Preflight {
        val failure = loadFailure
        if (failure != null) return Preflight(false, false, "Ponte nativa indisponível: $failure")
        if (stagedDirectory.isNullOrBlank()) return Preflight(true, false, "Selecione um APK preparado antes da análise.")
        val root = File(stagedDirectory)
        val abi = Build.SUPPORTED_ABIS.firstOrNull { candidate ->
            val libDir = File(File(root, "lib"), candidate)
            listOf("libc++_shared.so", "libfmod.so", "libMediaDecoders_Android.so", "libminecraftpe.so")
                .all { File(libDir, it).isFile && File(libDir, it).length() > 0L }
        }
        if (abi == null) return Preflight(true, false, "Nenhuma ABI do aparelho tem todas as bibliotecas obrigatórias. O runtime não pode ser iniciado.")
        return try {
            val result = nativePreflight(stagedDirectory, abi)
            Preflight(true, false, result.substringAfter(':', result))
        } catch (error: Throwable) {
            Preflight(true, false, "Pré-verificação nativa falhou: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    @JvmStatic private external fun nativePreflight(stagedDirectory: String, abi: String): String
}
