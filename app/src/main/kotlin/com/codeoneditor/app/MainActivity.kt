package com.codeoneditor.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codeoneditor.app.ui.theme.MyApplicationTheme
import com.codeoneditor.app.runtime.RuntimeStorage
import com.codeoneditor.app.runtime.RuntimeCompatibility
import com.codeoneditor.app.runtime.RuntimeBootstrapper
import com.codeoneditor.app.runtime.NativeRuntimeBridge
import com.codeoneditor.app.runtime.RuntimeIsolationManager
import org.levimc.launcher.core.minecraft.ACTION_MAIN_ACTIVITY_FIRST_DRAWN
import org.levimc.launcher.core.minecraft.EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW
import org.levimc.launcher.preloader.PreloaderSignatureRulesManager
import org.levimc.launcher.util.LauncherStorage
import android.view.ViewTreeObserver
import java.io.File

private val Background = Color(0xFF090909)
private val SurfaceDark = Color(0xFF171717)
private val SurfaceAlt = Color(0xFF292929)
private val Accent = Color(0xFFE0E0E0)
private val Muted = Color(0xFFA3A3A3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        bootstrapLauncherStorage()
        closeRestartActivityAfterFirstDraw()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Accent,
                        onPrimary = Color(0xFF090909),
                        background = Background,
                        surface = SurfaceDark,
                        onSurface = Color(0xFFF2F5F2),
                        secondary = Accent
                    )
                ) { LauncherApp() }
            }
        }
    }

    /**
     * Mesmo aquecimento que o SplashActivity original fazia antes de chegar à tela principal.
     * Este Activity agora é a entrada LAUNCHER, então precisa executá-lo sozinho.
     */
    private fun bootstrapLauncherStorage() {
        val appContext = applicationContext
        Thread({
            runCatching { LauncherStorage.ensureNoMedia(appContext) }
        }, "launcher-storage-warmup").start()
        runCatching { PreloaderSignatureRulesManager.refreshOnLauncherStart(appContext) }
    }

    /**
     * Após o jogo fechar, LauncherRestartActivity espera o broadcast ACTION_MAIN_ACTIVITY_FIRST_DRAWN
     * para se encerrar. Só a Activity original o enviava; sem isso a tela de reinício ficava presa.
     */
    private fun closeRestartActivityAfterFirstDraw() {
        if (!intent.getBooleanExtra(EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW, false)) return
        intent.removeExtra(EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW)
        val root = window.decorView
        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (root.viewTreeObserver.isAlive) {
                    root.viewTreeObserver.removeOnPreDrawListener(this)
                }
                root.post {
                    sendBroadcast(Intent(ACTION_MAIN_ACTIVITY_FIRST_DRAWN).setPackage(packageName))
                }
                return true
            }
        })
    }
}

private enum class LauncherPage(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Início", Icons.Default.Home),
    MODS("Mods", Icons.Default.Extension),
    WORLDS("Mundos", Icons.Default.Public),
    PROFILES("Perfis", Icons.Default.Layers),
    BACKUPS("Backups", Icons.Default.Security),
    SETTINGS("Configurações", Icons.Default.Settings)
}

@Composable
private fun LauncherApp() {
    val context = LocalContext.current
    var page by remember { mutableStateOf(LauncherPage.HOME) }
    var message by remember { mutableStateOf("Pronto para configurar seu Minecraft.") }
    var importedPacks by remember { mutableStateOf(listOf<String>()) }
    var installed by remember { mutableStateOf(false) }
    val runtimeStorage = remember(context) { RuntimeStorage(context) }
    val runtimeCompatibility = remember(context) { RuntimeCompatibility(context) }
    val runtimeBootstrapper = remember(context) { RuntimeBootstrapper(context) }
    val isolationManager = remember(context) { RuntimeIsolationManager(context) }
    var isolatedProfileSummary by remember { mutableStateOf("Nenhum perfil isolado criado.") }
    var stagedRuntimeSummary by remember { mutableStateOf("Nenhum APK foi preparado para o runtime privado.") }
    var stagedRuntimeDirectory by remember { mutableStateOf<String?>(null) }
    var nativeBridgeSummary by remember { mutableStateOf("Pré-verificação nativa ainda não executada.") }
    var compatibilitySummary by remember { mutableStateOf("Diagnóstico de compatibilidade ainda não executado.") }
    var storageRoot by remember { mutableStateOf("") }
    var storageReady by remember { mutableStateOf(false) }

    LaunchedEffect(runtimeStorage) {
        runtimeStorage.prepare().onSuccess { status ->
            storageRoot = status.rootPath
            storageReady = status.writable
            message = "Estrutura privada preparada (${status.directoryCount} pastas). O runtime isolado ainda não está integrado."
        }.onFailure { error ->
            storageReady = false
            message = "Falha ao preparar armazenamento: ${error.message ?: "erro desconhecido"}"
        }
    }

    val importRuntimeApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { runtimeBootstrapper.stageOfficialApk(uri) }
                .onSuccess { result ->
                    stagedRuntimeDirectory = result.stagedDirectory
                    stagedRuntimeSummary = "Minecraft ${result.versionName} (código ${result.versionCode}) • ABI: ${result.extractedAbis.joinToString()} • SHA-256: ${result.sha256.take(16)}… • ${result.stagedDirectory}"
                    message = result.message
                }
                .onFailure { error ->
                    stagedRuntimeDirectory = null
                    stagedRuntimeSummary = "Preparação falhou: ${error.message ?: "erro desconhecido"}"
                    message = stagedRuntimeSummary
                }
        }
    }

    val importPack = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val name = (uri.lastPathSegment ?: "pacote").substringAfterLast('/')
                    .substringAfterLast(':').ifBlank { "pacote" }
                val folder = runtimeStorage.imports.apply { mkdirs() }
                val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val out = File(folder, safeName)
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Não foi possível ler o arquivo selecionado." }.use { source ->
                        out.outputStream().use { target -> source.copyTo(target) }
                    }
                }
                importedPacks = (importedPacks + out.name).distinct()
                message = "Pacote importado para o armazenamento privado do launcher."
            }.onFailure { message = "Falha ao importar: ${it.message ?: "erro desconhecido"}" }
        }
    }

    Row(Modifier.fillMaxSize().background(Background).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.width(166.dp).fillMaxHeight().background(SurfaceDark, RoundedCornerShape(18.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                Box(Modifier.size(34.dp).background(Accent, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color.Black)
                }
                Column {
                    Text("BEDROCK", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                    Text("LAUNCHER", fontSize = 10.sp, color = Accent, letterSpacing = 2.sp)
                }
            }
            LauncherPage.entries.forEach { item ->
                val selected = page == item
                Row(
                    Modifier.fillMaxWidth().background(if (selected) SurfaceAlt else Color.Transparent, RoundedCornerShape(11.dp))
                        .clickable { page = item }.padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(item.icon, contentDescription = null, tint = if (selected) Accent else Muted, modifier = Modifier.size(19.dp))
                    Text(item.label, color = if (selected) Color.White else Muted, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("VERSÃO INICIAL • 0.1", color = Muted, fontSize = 10.sp)
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(page.label.uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Seu espaço para jogar e organizar conteúdo", color = Muted, fontSize = 12.sp)
                }
                Surface(color = SurfaceDark, shape = RoundedCornerShape(50)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(7.dp).background(if (installed) Accent else Color(0xFFFFC857), RoundedCornerShape(50)))
                        Text(if (installed) "Minecraft detectado" else "Verifique a instalação", color = Muted, fontSize = 11.sp)
                    }
                }
            }
            when (page) {
                LauncherPage.HOME -> HomePage(
                    onPlay = {
                        val activity = context as? android.app.Activity
                        if (activity == null) {
                            message = "Não foi possível iniciar: contexto sem Activity."
                        } else {
                            runCatching {
                                val manager = org.levimc.launcher.core.versions.VersionManager.get(activity)
                                // Mesma ordem do launcher original: selecionada → instalada → importada.
                                val version = manager.selectedVersion
                                    ?: manager.installedVersions.firstOrNull()
                                    ?: manager.customVersions.firstOrNull()
                                if (version == null) {
                                    message = "Nenhuma versão do Minecraft encontrada. Instale o Minecraft pela Play Store."
                                } else if (!org.levimc.launcher.util.PlayStoreValidator.isMinecraftFromPlayStore(activity)) {
                                    org.levimc.launcher.ui.dialogs.PlayStoreValidationDialog.showNotFromPlayStoreDialog(activity)
                                    message = "O Minecraft instalado não foi verificado como versão da Play Store."
                                } else {
                                    // Isolamento só para versões importadas; a instalação oficial usa o perfil dela.
                                    if (!version.isInstalled) version.versionIsolation = true
                                    val trace = org.levimc.launcher.core.minecraft.LaunchTrace.create(null)
                                    trace.milestone("Launch requested")
                                    val launchIntent = Intent()
                                    launchIntent.putExtra(
                                        org.levimc.launcher.core.minecraft.LaunchTrace.EXTRA_SESSION_ID,
                                        trace.sessionId
                                    )
                                    org.levimc.launcher.core.minecraft.MinecraftLauncher(activity)
                                        .launch(launchIntent, version, object : org.levimc.launcher.core.minecraft.MinecraftLauncher.LaunchCallback {
                                            override fun onLaunchStarted() {
                                                activity.runOnUiThread { message = "Abrindo ${version.displayName}..." }
                                            }
                                            override fun onLaunchFailed(e: Exception) {
                                                activity.runOnUiThread { message = "Não foi possível iniciar: ${e.message ?: e.javaClass.simpleName}" }
                                            }
                                        })
                                }
                            }.onFailure { error ->
                                message = "Falha ao preparar a inicialização: ${error.message ?: error.javaClass.simpleName}"
                            }
                        }
                    },
                    onMods = { page = LauncherPage.MODS },
                    onWorlds = { page = LauncherPage.WORLDS },
                    message = message
                )
                LauncherPage.MODS -> ModsPage(importedPacks, onImport = {
                    importPack.launch(arrayOf("*/*"))
                }, onMessage = { message = it })
                LauncherPage.WORLDS -> InfoPage(
                    icon = Icons.Default.Public,
                    title = "Mundos e dados",
                    body = "O acesso aos mundos depende da versão do Minecraft e das permissões de armazenamento do Android. Esta versão ainda não lê nem modifica os mundos automaticamente.",
                    points = listOf("Não alteramos os arquivos dos mundos por conta própria.", "Faça uma cópia pelo próprio Minecraft antes de testar conteúdo novo.", "A integração de backup será adicionada em uma etapa posterior.")
                )
                LauncherPage.PROFILES -> ProfilesPage(
                    currentSummary = isolatedProfileSummary,
                    onCreate = { name ->
                        runCatching { isolationManager.createProfile(name) }
                            .onSuccess { profile ->
                                val validation = isolationManager.validate(profile).getOrThrow()
                                isolatedProfileSummary = "${profile.name} • ${profile.id} • $validation"
                                message = "Perfil privado criado. O Minecraft ainda não está conectado a ele."
                            }
                            .onFailure { error ->
                                isolatedProfileSummary = "Falha: ${error.message ?: error.javaClass.simpleName}"
                                message = isolatedProfileSummary
                            }
                    }
                )
                LauncherPage.BACKUPS -> InfoPage(
                    icon = Icons.Default.Security,
                    title = "Proteção e backups",
                    body = "A proteção real depende de cópias de segurança verificadas. Esta tela informa o estado atual sem prometer bloqueios que ainda não existem.",
                    points = listOf("Backup automático: ainda não implementado", "Restauração automática: ainda não implementada", "Recomendação: mantenha cópias dos mundos fora do dispositivo.")
                )
                LauncherPage.SETTINGS -> SettingsPage(
                    onMessage = { message = it },
                    storageRoot = storageRoot,
                    storageReady = storageReady,
                    directories = runtimeStorage.describe().map { (name, file) -> "$name: ${if (file.exists()) "OK" else "pendente"}" },
                    compatibilitySummary = compatibilitySummary,
                    stagedRuntimeSummary = stagedRuntimeSummary,
                    nativeBridgeSummary = nativeBridgeSummary,
                    onNativePreflight = {
                        val result = NativeRuntimeBridge.inspect(stagedRuntimeDirectory)
                        nativeBridgeSummary = result.message
                        message = if (result.readyToLaunch) "Pré-verificação aprovada." else "Inicialização continua bloqueada: a ponte ainda não confirma um runtime isolado funcional."
                    },
                    onStageRuntimeApk = { importRuntimeApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")) },
                    onInspectRuntime = {
                        val report = runtimeCompatibility.inspect()
                        compatibilitySummary = buildString {
                            append("Android API ${report.androidSdk} • ABI: ${report.supportedAbis.joinToString().ifBlank { "não detectada" }} • ")
                            append(if (report.officialMinecraftInstalled) "Minecraft instalado" else "Minecraft oficial não encontrado")
                            report.minecraftVersion?.let { append(" • versão $it") }
                            append(". Isso é apenas diagnóstico; não confirma compatibilidade com runtime isolado.")
                        }
                        message = compatibilitySummary
                    }
                )
            }
            Text(message, color = Muted, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun HomePage(onPlay: () -> Unit, onMods: () -> Unit, onWorlds: () -> Unit, message: String) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.weight(1.55f).fillMaxHeight().background(SurfaceDark, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SUA PRÓXIMA AVENTURA", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Minecraft Bedrock", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Jogue, organize seus pacotes e mantenha seus dados sob controle.", color = Muted, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onPlay, modifier = Modifier.height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black), shape = RoundedCornerShape(13.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("JOGAR", fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = onMods, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(13.dp)) {
                    Icon(Icons.Default.Extension, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("GERENCIAR MODS")
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard(Icons.Default.Extension, "Biblioteca de mods", "Importe arquivos .mcpack e .mcaddon para a área privada do launcher.", onMods, Modifier.weight(1f))
            QuickCard(Icons.Default.Public, "Seus mundos", "Orientações para manter seus mundos seguros.", onWorlds, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.size(28.dp).background(SurfaceAlt, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
    }
}

@Composable
private fun ModsPage(importedPacks: List<String>, onImport: () -> Unit, onMessage: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(SurfaceDark, RoundedCornerShape(18.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pacotes importados", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Os arquivos ficam no armazenamento privado deste aplicativo.", color = Muted, fontSize = 12.sp)
            }
            Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Importar pacote")
            }
        }
        if (importedPacks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = Muted, modifier = Modifier.size(26.dp))
                    Text("Sua biblioteca está vazia", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Selecione um arquivo .mcpack ou .mcaddon para armazená-lo.", color = Muted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(importedPacks) { pack ->
                    Row(Modifier.fillMaxWidth().background(SurfaceAlt, RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(10.dp))
                        Text(pack, color = Color.White, modifier = Modifier.weight(1f))
                        Text("Importado", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        Text("Importar não ativa o pacote no Minecraft. A ativação e a validação de compatibilidade serão implementadas separadamente.", color = Color(0xFFFFD27D), fontSize = 11.sp)
    }
}

@Composable
private fun ProfilesPage(currentSummary: String, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("Meu perfil") }
    Column(
        Modifier.fillMaxSize().background(SurfaceDark, RoundedCornerShape(16.dp)).verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Perfis isolados", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Cada perfil recebe pastas privadas vazias para dados, mundos, pacotes e logs. O carregador ainda precisa redirecionar o Minecraft para essas pastas.", color = Muted, fontSize = 11.sp)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do perfil") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onCreate(name) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Criar pasta isolada", fontSize = 12.sp)
        }
        Text("RESULTADO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(currentSummary, color = Muted, fontSize = 10.sp)
        Text("Estado: armazenamento isolado preparado; isolamento em execução NÃO validado. O botão Jogar permanece bloqueado.", color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun InfoPage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    points: List<String>
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().background(SurfaceDark, RoundedCornerShape(18.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Muted, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f).fillMaxHeight().background(SurfaceDark, RoundedCornerShape(18.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("ESTADO ATUAL", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            points.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                    Text(point, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(
    onMessage: (String) -> Unit,
    storageRoot: String,
    storageReady: Boolean,
    directories: List<String>,
    compatibilitySummary: String,
    stagedRuntimeSummary: String,
    nativeBridgeSummary: String,
    onNativePreflight: () -> Unit,
    onStageRuntimeApk: () -> Unit,
    onInspectRuntime: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(SurfaceDark, RoundedCornerShape(18.dp)).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Preferências do launcher", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().background(SurfaceAlt, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Orientação", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Paisagem (horizontal), configurada para este aplicativo.", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Row(Modifier.fillMaxWidth().background(SurfaceAlt, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Armazenamento", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Pastas de runtime preparadas no armazenamento privado do launcher.", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.Folder, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.fillMaxWidth().background(SurfaceAlt, RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("AMBIENTE DE EXECUÇÃO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(if (storageReady) "Estrutura privada pronta" else "Estrutura privada indisponível", color = if (storageReady) Color.White else Muted, fontSize = 12.sp)
            if (storageRoot.isNotBlank()) Text(storageRoot, color = Muted, fontSize = 10.sp)
            directories.forEach { item -> Text("• $item", color = Muted, fontSize = 11.sp) }
            Text("Preparar essas pastas não significa que o Minecraft já esteja isolado ou que mods nativos sejam carregados.", color = Muted, fontSize = 11.sp)
        }
        Column(Modifier.fillMaxWidth().background(SurfaceAlt, RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PREPARAÇÃO DO RUNTIME PRIVADO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(stagedRuntimeSummary, color = Muted, fontSize = 11.sp)
            Text("Confere o identificador do pacote, calcula SHA-256 e extrai bibliotecas compatíveis para uma pasta privada. Isso não verifica a assinatura do APK. Não instala nem inicia o jogo e não copia mundos.", color = Muted, fontSize = 11.sp)
            OutlinedButton(onClick = onStageRuntimeApk) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Selecionar APK para preparar", fontSize = 11.sp)
            }
        }
        Column(Modifier.fillMaxWidth().background(SurfaceAlt, RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PONTE NATIVA (JNI)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(nativeBridgeSummary, color = Muted, fontSize = 10.sp)
            Text("Esta ponte faz apenas pré-verificação de arquivos. Não inicia o Minecraft e mantém o botão Jogar bloqueado até existir um carregador validado.", color = Muted, fontSize = 10.sp)
            OutlinedButton(onClick = onNativePreflight, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Testar ponte nativa", fontSize = 11.sp)
            }
        }
        Column(Modifier.fillMaxWidth().background(SurfaceAlt, RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("COMPATIBILIDADE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(compatibilitySummary, color = Muted, fontSize = 11.sp)
        }
        OutlinedButton(onClick = {
            onInspectRuntime()
            onMessage("Diagnóstico concluído. Confira o resultado em Compatibilidade.")
        }) {
            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Verificar compatibilidade do dispositivo", fontSize = 11.sp)
        }
    }
}
