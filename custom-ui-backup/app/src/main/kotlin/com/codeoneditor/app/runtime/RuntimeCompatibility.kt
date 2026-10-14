package com.codeoneditor.app.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Read-only checks used before attempting any imported-APK/runtime integration. */
class RuntimeCompatibility(private val context: Context) {
    data class Report(
        val androidSdk: Int,
        val supportedAbis: List<String>,
        val officialMinecraftInstalled: Boolean,
        val minecraftVersion: String?,
        val packageName: String = MINECRAFT_PACKAGE
    ) {
        val nativeRuntimeCanBeIntegrated: Boolean
            get() = supportedAbis.isNotEmpty() && officialMinecraftInstalled
    }

    fun inspect(): Report {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    MINECRAFT_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(MINECRAFT_PACKAGE, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: RuntimeException) {
            null
        }

        return Report(
            androidSdk = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            officialMinecraftInstalled = packageInfo != null,
            minecraftVersion = packageInfo?.versionName
        )
    }

    companion object {
        const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
    }
}
