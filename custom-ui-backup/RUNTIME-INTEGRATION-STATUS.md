# Bedrock Launcher — runtime integration status (Etapa 5)

## What was adapted in this step

- Native preflight now checks for the four primary Bedrock native libraries (`libc++_shared.so`, `libfmod.so`, `libMediaDecoders_Android.so`, and `libminecraftpe.so`) under a device-supported ABI.
- Preflight remains fail-closed. Even when these files exist, it does **not** claim that Minecraft can launch.
- Added a per-profile, app-private directory tree with `files`, `data`, `cache`, `worlds`, `resource_packs`, `behavior_packs`, `mods`, `logs`, and `backups`, plus a manifest declaring that no original worlds were copied.
- Added an explicit profile validation operation.
- Reduced density of several landscape UI elements and added a working profile-creation screen.
- Added Apache 2.0 attribution for the upstream LeviLaunchroid reference project.

## Why the full loader is not a safe copy/paste

The reference launcher does not launch Minecraft by simply calling JNI. Its path includes at least:

1. `MinecraftLauncher` to build a launch intent and storage-path extras;
2. `MinecraftLoadingActivity` to prepare asynchronously and report errors;
3. `MinecraftRuntimePreparer` to resolve a version, configure the launch intent, load mods/preloader, and load libraries;
4. `GamePackageManager` to create a package context/asset manager and extract/cache required libraries;
5. `MinecraftActivity`, which subclasses Mojang's `com.mojang.minecraftpe.MainActivity`;
6. additional classes for `GameVersion`, `LauncherStorage`, `MinecraftLaunchSession`, tracing, resource packs, auth, and native preloader/mod loading.

This repository currently lacks that class graph and the Minecraft `MainActivity` compile/runtime dependency. Adding only the upstream launcher classes would not compile or provide a functional runtime. A staged APK is also not enough to guarantee all split APKs/resources are present.

## Data-safety rule

The app does not write to the official Minecraft private data directory and does not copy worlds automatically. The new profile directories are empty, app-private directories. **This does not yet prove runtime isolation**, because the embedded game activity and native loader have not been wired to use these paths. The Play action must remain blocked until that integration is complete and verified on-device.

## Validation

- Check archive integrity with `unzip -t`.
- Android build/device runtime validation remains pending if Gradle/Android SDK dependencies cannot be downloaded in the build environment.
- Do not describe this stage as a working Minecraft launcher or as verified achievements/world isolation.
