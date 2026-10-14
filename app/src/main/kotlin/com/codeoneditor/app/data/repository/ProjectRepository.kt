package com.codeoneditor.app.data.repository

import android.content.Context
import android.util.Base64
import android.content.SharedPreferences
import org.json.JSONArray
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.codeoneditor.app.data.model.FileItem
import com.codeoneditor.core.editor.model.FileType
import com.codeoneditor.app.data.model.AppSettingsState
import com.codeoneditor.core.editor.model.EditorTab
import com.codeoneditor.app.data.model.ProjectItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectRepository {
    private val _projects = MutableStateFlow<List<ProjectItem>>(emptyList())
    val projects: StateFlow<List<ProjectItem>> = _projects.asStateFlow()
    private val _activeProject = MutableStateFlow<ProjectItem?>(null)
    val activeProject: StateFlow<ProjectItem?> = _activeProject.asStateFlow()
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()
    private val _settings = MutableStateFlow(AppSettingsState())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()
    private var storageRootDir: File? = null
    private var appFilesDir: File? = null
    private var settingsPrefs: SharedPreferences? = null
    private var appContext: Context? = null
    private val _loadingNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingNodeIds: StateFlow<Set<String>> = _loadingNodeIds.asStateFlow()
    private val projectOpenSnapshots = mutableMapOf<String, ProjectItem>()

    init {
        // App starts empty with NO pre-created or demo projects
        _projects.value = emptyList()
        _activeProject.value = null
        _openTabs.value = emptyList()
        _activeTabId.value = null
    }

    fun initStorage(filesDir: File) {
        try {
            appFilesDir = filesDir
            storageRootDir = File(filesDir, "workspace").apply { mkdirs() }
            loadProjectsIndex()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }    /**
     * Recarrega a lista de projetos salva em disco (projects_index.json).
     * Sem isso, os projetos criados desapareciam ao sair e reabrir o app,
     * pois a lista só existia em memória (_projects começava vazia sempre).
     */
    private fun loadProjectsIndex() {
        try {
            val indexFile = File(appFilesDir ?: return, "projects_index.json")
            if (!indexFile.exists()) return
            val json = indexFile.readText()
            if (json.isBlank()) return
            val array = JSONArray(json)
            val loaded = mutableListOf<ProjectItem>()
            for (i in 0 until array.length()) {
                loaded.add(ProjectJsonMapper.projectFromJson(array.getJSONObject(i)))
            }
            _projects.value = loaded
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }    /**
     * Salva a lista completa de projetos (metadados + árvore de arquivos)
     * em disco, para que fiquem disponíveis na próxima abertura do app.
     */
    private fun persistProjectsIndex() {
        try {
            val indexFile = File(appFilesDir ?: return, "projects_index.json")
            val array = JSONArray()
            _projects.value.forEach { array.put(ProjectJsonMapper.projectToJson(it)) }
            val serialized = array.toString()
            // Write to a sibling temporary file first. If the process is
            // interrupted while persisting the index, the previous valid
            // index remains intact instead of becoming a truncated JSON file.
            val tempFile = File(indexFile.parentFile, "${indexFile.name}.tmp")
            tempFile.writeText(serialized)
            if (!tempFile.renameTo(indexFile)) {
                // Some filesystems do not replace an existing target through
                // renameTo(). Fall back only after the complete temp file exists.
                if (indexFile.exists() && !indexFile.delete()) {
                    throw IllegalStateException("Não foi possível substituir o índice de projetos")
                }
                if (!tempFile.renameTo(indexFile)) {
                    throw IllegalStateException("Não foi possível finalizar o índice de projetos")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

                    /**
     * Define o diretório escolhido pelo usuário como local do projeto.
     * O caminho é mantido para que a pasta do projeto seja criada dentro
     * do local mostrado na tela de novo projeto.
     */
    fun setStorageRoot(path: String) {
        try {
            val cleanPath = path.trim().trimEnd('/')
            if (cleanPath.isBlank()) return
            val selected = File(cleanPath)
            if (!selected.exists()) selected.mkdirs()
            if (selected.isDirectory) storageRootDir = selected
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Carrega as preferências salvas em disco (SharedPreferences) e
     * aplica ao estado atual. Deve ser chamada uma vez na inicialização
     * do app. Sem isso, as configurações (tema, auto-save etc.) só
     * existiam em memória e voltavam ao padrão a cada reabertura do app.
     */
    fun loadPersistedSettings(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        settingsPrefs = prefs

        _settings.update { current ->
            current.copy(
                themeMode = prefs.getString("theme_mode", current.themeMode) ?: current.themeMode,
                autoSave = prefs.getBoolean("auto_save", current.autoSave),
                openLastProjectOnStart = prefs.getBoolean("open_last_project", current.openLastProjectOnStart),
                showLineNumbers = prefs.getBoolean("show_line_numbers", current.showLineNumbers),
                wordWrap = prefs.getBoolean("word_wrap", current.wordWrap),
                fontSize = prefs.getInt("editor_font_size", current.fontSize),
                tabSize = prefs.getInt("editor_tab_size", current.tabSize),
                showWhitespace = prefs.getBoolean("editor_show_whitespace", current.showWhitespace),
                autoCloseBrackets = prefs.getBoolean("auto_close_brackets", current.autoCloseBrackets),
                pinLineNumbers = prefs.getBoolean("editor_pin_line_numbers", current.pinLineNumbers),
                highlightCurrentLine = prefs.getBoolean("editor_highlight_current_line", current.highlightCurrentLine),
                ligatures = prefs.getBoolean("editor_ligatures", current.ligatures),
            )
        }
    }

    private fun persistSettings(state: AppSettingsState) {
        val prefs = settingsPrefs ?: return
        prefs.edit()
            .putString("theme_mode", state.themeMode)
            .putBoolean("auto_save", state.autoSave)
            .putBoolean("open_last_project", state.openLastProjectOnStart)
            .putBoolean("show_line_numbers", state.showLineNumbers)
            .putBoolean("word_wrap", state.wordWrap)
            .putInt("editor_font_size", state.fontSize)
            .putInt("editor_tab_size", state.tabSize)
            .putBoolean("editor_show_whitespace", state.showWhitespace)
            .putBoolean("auto_close_brackets", state.autoCloseBrackets)
            .putBoolean("editor_pin_line_numbers", state.pinLineNumbers)
            .putBoolean("editor_highlight_current_line", state.highlightCurrentLine)
            .putBoolean("editor_ligatures", state.ligatures)
            .apply()
    }

    fun persistFileToDisk(projectName: String, relativePath: String, content: String): Boolean {
        return try {
            val project = _activeProject.value
            val item = project?.let { ProjectTreeOperations.findNodeByPath(it.files, relativePath) }
            val context = appContext
            if (project?.sourceUri != null && item?.sourceUri != null && context != null) {
                val externalSaved = runCatching {
                    context.contentResolver.openOutputStream(Uri.parse(item.sourceUri), "wt")?.use {
                        it.write(content.toByteArray(Charsets.UTF_8))
                    } != null
                }.getOrDefault(false)
                if (externalSaved) return true
                // If the external provider is no longer available after an
                // update, continue to the private project copy below.
            }

            val root = project?.let { ProjectTreeOperations.projectRootDir(it, storageRootDir) } ?: return false
            val cleanRelPath = relativePath.removePrefix("$projectName/").trim('/')
            val targetFile = File(root, cleanRelPath)
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().buffered().use { it.write(content.toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    

    private fun persistProjectTreeToDisk(project: ProjectItem) {
        fun persistRecursive(files: List<FileItem>) {
            for (item in files) {
                if (item.isDirectory) {
                    ProjectTreeOperations.projectRootDir(project, storageRootDir)?.let { root ->
                        val cleanRelPath = if (item.path.startsWith("${project.name}/")) {
                            item.path.removePrefix("${project.name}/")
                        } else item.path
                        File(root, cleanRelPath).mkdirs()
                    }
                    persistRecursive(item.children)
                } else {
                    persistFileToDisk(project.name, item.path, item.content)
                }
            }
        }
        persistRecursive(project.files)
    }

    fun autoSaveDirtyTabs(): Boolean {
        val sourceProj = _activeProject.value ?: return false
        val proj = ProjectTreeOperations.deepCopyProject(sourceProj)
        val currentTabs = _openTabs.value
        val dirtyTabs = currentTabs.filter { it.isDirty }
        if (dirtyTabs.isEmpty()) return false

        val savedIds = dirtyTabs.mapNotNull { tab ->
            ProjectTreeOperations.updateFileContentInTree(proj.files, tab.fileId, tab.content)
            if (persistFileToDisk(proj.name, tab.filePath, tab.content)) tab.fileId else null
        }.toSet()

        _openTabs.update { tabs ->
            tabs.map { tab -> if (tab.fileId in savedIds) tab.copy(isDirty = false) else tab }
        }
        _activeProject.value = proj.copy()
        return true
    }

    fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        _settings.update(transform)
        persistSettings(_settings.value)
    }

    private fun activateProject(project: ProjectItem) {
        projectOpenSnapshots[project.id] = ProjectTreeOperations.deepCopyProject(project)
        settingsPrefs?.edit()?.putString("last_project_id", project.id)?.apply()
        _activeProject.value = project
        _openTabs.value = emptyList()
        _activeTabId.value = null

    }

    /**
     * Abre um projeto salvo usando o filesystem atual como autoridade.
     * O índice persistido serve apenas para descobrir o projeto; ele nunca
     * é tratado como a fonte definitiva da árvore.
     */
    suspend fun openProject(project: ProjectItem): Result<ProjectItem> = runCatching {
        val refreshed = withContext(Dispatchers.IO) { refreshProjectFromStorage(project) }
        val updatedProjects = _projects.value.map { if (it.id == project.id) refreshed else it }
        _projects.value = updatedProjects
        persistProjectsIndex()
        activateProject(refreshed)
        refreshed
    }

    fun selectProject(project: ProjectItem) {
        activateProject(project)
    }

    suspend fun restoreLastProjectIfEnabled(): ProjectItem? {
        if (!_settings.value.openLastProjectOnStart) return null
        val id = settingsPrefs?.getString("last_project_id", null) ?: return null
        val project = _projects.value.firstOrNull { it.id == id } ?: return null
        return openProject(project).getOrNull()
    }

    private fun refreshProjectFromStorage(project: ProjectItem): ProjectItem {
        val existingRoot = project.files.firstOrNull { it.isDirectory && it.path == project.name }
        val root = existingRoot ?: FileItem(
            name = project.name,
            path = project.name,
            isDirectory = true,
            isExpanded = true,
            sourceUri = project.sourceUri,
            childrenLoaded = false
        )
        val freshChildren = runCatching { loadDirectoryChildren(project, root) }.getOrElse {
            if (project.sourceUri != null && project.rootPath != null) {
                loadInternalChildrenFromRoot(project, root)
            } else throw it
        }
        val mergedChildren = mergeDirectoryChildren(root.children, freshChildren)
        val refreshedRoot = root.copy(
            isExpanded = true,
            children = mergedChildren,
            sourceUri = project.sourceUri,
            childrenLoaded = true
        )
        return project.copy(files = mutableListOf(refreshedRoot))
    }

    fun createProject(
        name: String,
        packageName: String,
        language: String,
        minSdk: Int = 24
    ): ProjectItem {
        val now = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        val project = createDefaultProject(name, language, packageName, now, minSdk)
        _projects.update { listOf(project) + it }
        persistProjectTreeToDisk(project)
        storageRootDir?.let { root ->
        }
        persistProjectsIndex()
        selectProject(project)
        return project
    }

    /**
     * Importa um projeto a partir de uma pasta real escolhida pelo usuário no
     * gerenciador de arquivos do Android (Storage Access Framework).
     *
     * Lê a árvore de pastas/arquivos de verdade a partir da [treeUri], monta a
     * estrutura de [ProjectItem]/[FileItem] correspondente, copia o conteúdo
     * para o armazenamento interno do app (mesmo esquema usado pelos projetos
     * criados normalmente) e adiciona o projeto à lista. Retorna null se a
     * pasta não puder ser lida.
     */
    fun importProjectFromTree(context: Context, treeUri: Uri): ProjectItem? {
        return try {
            appContext = context.applicationContext
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }

            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            if (!rootDoc.isDirectory) return null

            val projectName = rootDoc.name?.takeIf { it.isNotBlank() } ?: "Projeto Importado"
            android.util.Log.d(SAF_LOG_TAG, "Importando projeto '$projectName' de $treeUri")

            val rootChildren = listSafChildren(context, treeUri, projectName)
            android.util.Log.d(SAF_LOG_TAG, "Raiz de '$projectName' tem ${rootChildren.size} itens reais")

            // Do not recursively copy the whole project here. Large projects can
            // contain thousands of files, and doing that before opening the editor
            // is the reason the bottom button can remain in "Abrindo..." for a long
            // time. The SAF URI is persisted and is the primary source; a private
            // backup is created asynchronously after the editor is already open.
            val localRoot = File(storageRootDir ?: context.filesDir, "workspace/$projectName")
            localRoot.mkdirs()

            val rootProjectNode = FileItem(
                name = projectName,
                path = projectName,
                isDirectory = true,
                isExpanded = true,
                children = rootChildren.toMutableList(),
                sourceUri = treeUri.toString(),
                childrenLoaded = true
            )

            val now = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            val project = ProjectItem(
                name = projectName,
                packageName = "com.codeoneditor.app.${projectName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }}",
                createdAt = now,
                                language = "Multi",
                minSdk = 24,
                files = mutableListOf(rootProjectNode),
                sourceUri = treeUri.toString(),
                rootPath = localRoot.absolutePath
            )

            val existing = _projects.value.firstOrNull { it.sourceUri == project.sourceUri }
            val effective = if (existing != null) project.copy(id = existing.id) else project
            _projects.update { current ->
                if (existing == null) listOf(effective) + current
                else current.map { if (it.id == existing.id) effective else it }
            }
            persistProjectsIndex()
            activateProject(effective)
            effective
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun copySafTreeToInternal(context: Context, source: DocumentFile, destination: File) {
        if (!source.isDirectory) return
        destination.mkdirs()
        source.listFiles().forEach { child ->
            val name = child.name?.takeIf { it.isNotBlank() } ?: return@forEach
            val target = File(destination, name)
            if (child.isDirectory) {
                copySafTreeToInternal(context, child, target)
            } else {
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    target.parentFile?.mkdirs()
                    target.outputStream().buffered().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    /**
     * Creates/refreshes the private backup without blocking project opening.
     * The external SAF tree remains the primary source while it is available.
     */
    suspend fun backupImportedProject(project: ProjectItem) {
        val context = appContext ?: return
        val sourceUri = project.sourceUri ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceRoot = DocumentFile.fromTreeUri(context, Uri.parse(sourceUri))
                    ?: return@runCatching
                if (!sourceRoot.isDirectory) return@runCatching
                val destination = File(
                    storageRootDir ?: context.filesDir,
                    "workspace/${project.name}"
                )
                destination.mkdirs()
                copySafTreeToInternal(context, sourceRoot, destination)

                val updated = project.copy(rootPath = destination.absolutePath)
                _projects.update { list ->
                    list.map { if (it.id == project.id) updated else it }
                }
                if (_activeProject.value?.id == project.id) {
                    _activeProject.value = updated
                }
                persistProjectsIndex()
            }.onFailure { error ->
                android.util.Log.w(SAF_LOG_TAG, "Backup local do projeto '${project.name}' falhou", error)
            }
        }
    }

    private fun listSafChildren(
        context: Context,
        parentUri: Uri,
        parentPath: String
    ): List<FileItem> {
        // Lista o diretório real via consulta direta ao ContentResolver, em
        // vez de DocumentFile.listFiles(). listFiles() enumera os filhos e
        // depois faz UMA consulta extra por filho só para saber se é pasta
        // (child.isDirectory), o que em alguns provedores de armazenamento
        // (proprietários de fabricante, apps de arquivos de terceiros)
        // falha ou trava de forma inconsistente para parte dos itens —
        // resultando exatamente no sintoma relatado: uma pasta aparece na
        // árvore mas o toque nela não expande nada. Buscando
        // DOCUMENT_ID + DISPLAY_NAME + MIME_TYPE em uma única consulta,
        // cada item real do diretório é resolvido de uma vez só.
        val parentDocumentId = runCatching {
            if (DocumentsContract.isDocumentUri(context, parentUri)) {
                DocumentsContract.getDocumentId(parentUri)
            } else {
                DocumentsContract.getTreeDocumentId(parentUri)
            }
        }.getOrNull()
            ?: throw IllegalStateException("Não foi possível resolver a pasta real: $parentPath")

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, parentDocumentId)
        val result = mutableListOf<FileItem>()

        try {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: continue
                    val isDir = cursor.getString(mimeIdx) == DocumentsContract.Document.MIME_TYPE_DIR
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, docId)
                    result += FileItem(
                        name = name,
                        path = "$parentPath/$name",
                        isDirectory = isDir,
                        fileType = ProjectTreeOperations.detectFileType(name, isDir),
                        sourceUri = childUri.toString(),
                        childrenLoaded = !isDir
                    )
                }
            } ?: android.util.Log.w(SAF_LOG_TAG, "Consulta de diretório retornou nula: $parentPath ($childrenUri)")
        } catch (e: Exception) {
            android.util.Log.e(SAF_LOG_TAG, "Falha ao listar pasta real: $parentPath ($childrenUri)", e)
            throw IllegalStateException("Não foi possível listar a pasta: $parentPath", e)
        }

        android.util.Log.d(SAF_LOG_TAG, "Pasta '$parentPath' listada com ${result.size} itens reais")

        return result
            .distinctBy { it.path }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
    }

    private fun loadInternalChildrenFromRoot(project: ProjectItem, directory: FileItem): List<FileItem> {
        val root = ProjectTreeOperations.projectRootDir(project, storageRootDir)
            ?: throw IllegalStateException("Cópia local do projeto não está disponível")
        val relative = directory.path.removePrefix("${project.name}").trim('/')
        val dir = if (relative.isBlank()) root else File(root, relative)
        if (!dir.isDirectory) throw IllegalStateException("Diretório local não encontrado: ${dir.path}")
        return dir.listFiles().orEmpty().map { file ->
            FileItem(
                name = file.name,
                path = if (relative.isBlank()) "${project.name}/${file.name}" else "${project.name}/$relative/${file.name}",
                isDirectory = file.isDirectory,
                fileType = ProjectTreeOperations.detectFileType(file.name, file.isDirectory),
                sourceUri = null,
                childrenLoaded = !file.isDirectory
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
    }

    private fun listInternalChildren(project: ProjectItem, directory: FileItem): List<FileItem> {
        val root = ProjectTreeOperations.projectRootDir(project, storageRootDir)
            ?: throw IllegalStateException("Diretório raiz do projeto não está disponível")
        val cleanPath = directory.path.removePrefix("${project.name}/").trim('/')
        val dir = if (cleanPath.isBlank()) root else File(root, cleanPath)
        if (!dir.isDirectory) {
            throw IllegalStateException("Diretório não encontrado: ${dir.absolutePath}")
        }
        val children = dir.listFiles()?.toList()
            ?: throw IllegalStateException("Não foi possível listar o diretório: ${dir.absolutePath}")
        return children.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) })).map { file ->
            FileItem(
                name = file.name,
                path = "${directory.path}/${file.name}",
                isDirectory = file.isDirectory,
                fileType = ProjectTreeOperations.detectFileType(file.name, file.isDirectory),
                sourceUri = null,
                childrenLoaded = !file.isDirectory
            )
        }
    }

    private fun loadDirectoryChildren(project: ProjectItem, directory: FileItem): List<FileItem> {
        val context = appContext ?: return emptyList()
        if (project.sourceUri != null && directory.sourceUri != null) {
            return runCatching {
                listSafChildren(context, Uri.parse(directory.sourceUri), directory.path)
            }.getOrElse {
                // The Android document provider can revoke or lose a tree grant.
                // Do not make the project disappear after an app update: use the
                // private backup when the original SAF tree is temporarily unavailable.
                if (project.rootPath != null) {
                    listInternalChildren(project, directory)
                } else {
                    throw it
                }
            }
        }
        return listInternalChildren(project, directory)
    }

    private fun mergeDirectoryChildren(existing: List<FileItem>, fresh: List<FileItem>): MutableList<FileItem> {
        val oldByPath = existing.associateBy { it.path }
        return fresh.map { node ->
            val old = oldByPath[node.path] ?: return@map node
            if (!node.isDirectory) {
                node.copy(id = old.id)
            } else {
                node.copy(
                    id = old.id,
                    isExpanded = old.isExpanded,
                    children = old.children,
                    childrenLoaded = old.childrenLoaded
                )
            }
        }.toMutableList()
    }

    /**
     * Clona um repositório Git hospedado no GitHub, baixando o snapshot do
     * branch informado (via codeload.github.com) e importando o conteúdo
     * como um novo projeto — mesmo esquema de armazenamento usado por
     * [createProject] e [importProjectFromTree]. Deve ser chamada fora da
     * thread principal (faz I/O de rede e disco).
     *
     * Retorna null se a URL não apontar para um repositório do GitHub, se
     * o download falhar, ou se nenhum branch candidato existir.
     */
    fun cloneRepository(
        context: Context,
        repoUrl: String,
        branch: String,
        saveLocation: String,
        username: String = "",
        token: String = ""
    ): ProjectItem? {
        return try {
            appContext = context.applicationContext
            setStorageRoot(saveLocation)
            val root = storageRootDir ?: return null
            val (owner, repoName) = parseGithubRepoUrl(repoUrl) ?: return null

            val branchCandidates = linkedSetOf<String>().apply {
                if (branch.isNotBlank()) add(branch.trim())
                add("main")
                add("master")
            }

            val tempZip = File(appFilesDir ?: root, "clone-${System.currentTimeMillis()}.zip")
            try {
                var downloaded = false
                for (candidate in branchCandidates) {
                    val downloadUrl = "https://codeload.github.com/$owner/$repoName/zip/refs/heads/$candidate"
                    if (downloadFile(downloadUrl, tempZip, username, token)) {
                        downloaded = true
                        break
                    }
                }
                if (!downloaded) return null

                val existingNames = _projects.value.map { it.name }.toSet()
                var projectName = repoName
                var suffix = 2
                while (projectName in existingNames) {
                    projectName = "$repoName ($suffix)"
                    suffix++
                }

                val projectDir = File(root, projectName)
                if (projectDir.exists()) return null
                projectDir.mkdirs()

                ZipInputStream(BufferedInputStream(FileInputStream(tempZip), DEFAULT_IO_BUFFER_SIZE)).use { zis ->
                    var entry = zis.nextEntry
                    var prefix: String? = null
                    val buffer = ByteArray(DEFAULT_IO_BUFFER_SIZE)
                    while (entry != null) {
                        val raw = entry.name.replace('\\', '/')
                        if (prefix == null) prefix = raw.substringBefore('/', raw).takeIf { raw.contains('/') }
                        val relative = if (prefix != null && raw.startsWith("$prefix/")) raw.removePrefix("$prefix/") else raw
                        val safe = relative.trim('/').takeIf { it.isNotBlank() }
                        if (safe != null) {
                            val target = File(projectDir, safe)
                            val projectCanonical = projectDir.canonicalFile
                            val targetCanonical = target.canonicalFile
                            if (!targetCanonical.path.startsWith(projectCanonical.path + File.separator)) {
                                throw IllegalStateException("Entrada ZIP inválida")
                            }
                            if (entry.isDirectory) {
                                target.mkdirs()
                            } else {
                                target.parentFile?.mkdirs()
                                target.outputStream().buffered(DEFAULT_IO_BUFFER_SIZE).use { output ->
                                    while (true) {
                                        val count = zis.read(buffer)
                                        if (count < 0) break
                                        if (count > 0) output.write(buffer, 0, count)
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val rootNode = FileItem(
                    name = projectName,
                    path = projectName,
                    isDirectory = true,
                    isExpanded = true,
                    childrenLoaded = true
                )
                val now = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                val project = ProjectItem(
                    name = projectName,
                    packageName = "com.codeoneditor.app.${projectName.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }}",
                    createdAt = now,
                    language = "Multi",
                    minSdk = 26,
                    files = mutableListOf(rootNode),
                    rootPath = projectDir.absolutePath
                )
                val rootChildren = listInternalChildren(project, rootNode)
                val completeProject = project.copy(files = mutableListOf(rootNode.copy(children = rootChildren.toMutableList())))
                _projects.update { listOf(completeProject) + it }
                persistProjectsIndex()
                selectProject(completeProject)
                completeProject
            } finally {
                tempZip.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extrai owner/repo de uma URL do GitHub (com ou sem "https://",
     * "www.", ou sufixo ".git"). Retorna null se a URL não for do GitHub.
     */
    private fun parseGithubRepoUrl(url: String): Pair<String, String>? {
        val trimmed = url.trim().removeSuffix(".git").removeSuffix("/")
        val regex = Regex("""^(?:https?://)?(?:www\.)?github\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)""")
        val match = regex.find(trimmed) ?: return null
        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
        if (owner.isBlank() || repo.isBlank()) return null
        return owner to repo
    }

    /**
     * Faz o download do snapshot para disco sem manter o ZIP inteiro em memória.
     */
    private fun downloadFile(
        urlString: String,
        destination: File,
        username: String = "",
        token: String = ""
    ): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "CodeOnEditor-App")
                if (token.isNotBlank()) {
                    if (username.isNotBlank()) {
                        val credentials = "${username.trim()}:${token.trim()}"
                        val encoded = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                        setRequestProperty("Authorization", "Basic $encoded")
                    } else {
                        setRequestProperty("Authorization", "Bearer ${token.trim()}")
                    }
                }
            }
            if (connection.responseCode !in 200..299) return false
            connection.inputStream.buffered(DEFAULT_IO_BUFFER_SIZE).use { input ->
                destination.parentFile?.mkdirs()
                destination.outputStream().buffered(DEFAULT_IO_BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(DEFAULT_IO_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count > 0) output.write(buffer, 0, count)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    fun exportActiveProject(context: Context, destinationUri: Uri): Boolean {
        val project = _activeProject.value ?: return false
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                ZipOutputStream(output.buffered()).use { zip ->
                    val buffer = ByteArray(DEFAULT_IO_BUFFER_SIZE)

                    fun addInternal(file: File, relativePath: String) {
                        if (file.isDirectory) {
                            val children = file.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) })).orEmpty()
                            if (children.isEmpty() && relativePath.isNotBlank()) {
                                zip.putNextEntry(ZipEntry("$relativePath/"))
                                zip.closeEntry()
                            }
                            children.forEach { child ->
                                val childPath = if (relativePath.isBlank()) child.name else "$relativePath/${child.name}"
                                addInternal(child, childPath)
                            }
                        } else {
                            zip.putNextEntry(ZipEntry(relativePath))
                            FileInputStream(file).buffered(DEFAULT_IO_BUFFER_SIZE).use { input ->
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    if (count > 0) zip.write(buffer, 0, count)
                                }
                            }
                            zip.closeEntry()
                        }
                    }

                    fun addSaf(document: DocumentFile, relativePath: String) {
                        if (document.isDirectory) {
                            val children = document.listFiles().sortedWith(compareBy({ !it.isDirectory }, { (it.name ?: "").lowercase(Locale.ROOT) }))
                            if (children.isEmpty() && relativePath.isNotBlank()) {
                                zip.putNextEntry(ZipEntry("$relativePath/"))
                                zip.closeEntry()
                            }
                            children.forEach { child ->
                                val childName = child.name ?: return@forEach
                                val childPath = if (relativePath.isBlank()) childName else "$relativePath/$childName"
                                addSaf(child, childPath)
                            }
                        } else {
                            val input = document.uri.let { context.contentResolver.openInputStream(it) } ?: return
                            zip.putNextEntry(ZipEntry(relativePath))
                            input.buffered(DEFAULT_IO_BUFFER_SIZE).use { stream ->
                                while (true) {
                                    val count = stream.read(buffer)
                                    if (count < 0) break
                                    if (count > 0) zip.write(buffer, 0, count)
                                }
                            }
                            zip.closeEntry()
                        }
                    }

                    if (project.sourceUri != null) {
                        val root = DocumentFile.fromTreeUri(context, Uri.parse(project.sourceUri)) ?: return false
                        root.listFiles().sortedWith(compareBy({ !it.isDirectory }, { (it.name ?: "").lowercase(Locale.ROOT) })).forEach { child ->
                            val childName = child.name ?: return@forEach
                            addSaf(child, childName)
                        }
                    } else {
                        val root = ProjectTreeOperations.projectRootDir(project, storageRootDir) ?: return false
                        if (!root.exists()) return false
                        root.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) })).orEmpty().forEach { child ->
                            addInternal(child, child.name)
                        }
                    }
                }
            } != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun openFileInputStream(context: Context, project: ProjectItem, item: FileItem): java.io.InputStream? {
        if (project.sourceUri != null && item.sourceUri != null) {
            val external = runCatching {
                context.contentResolver.openInputStream(Uri.parse(item.sourceUri))
            }.getOrNull()
            if (external != null) return external
            // Fall back to the private copy when the persisted SAF grant is no
            // longer usable after an app/provider update.
        }
        val root = ProjectTreeOperations.projectRootDir(project, storageRootDir) ?: return null
        val cleanPath = item.path.removePrefix("${project.name}/")
        return FileInputStream(File(root, cleanPath))
    }

    private fun readFileContent(context: Context, project: ProjectItem, item: FileItem): String {
        val input = openFileInputStream(context, project, item) ?: return ""
        input.use { stream ->
            val buffered = BufferedInputStream(stream, DEFAULT_IO_BUFFER_SIZE)
            val bytes = buffered.readNBytes(MAX_EDITOR_FILE_BYTES + 1)
            if (bytes.size > MAX_EDITOR_FILE_BYTES) {
                throw IllegalStateException("Arquivo grande demais para edição: ${item.name}")
            }
            if (looksBinary(bytes)) {
                throw IllegalStateException("Arquivo binário não pode ser aberto no editor: ${item.name}")
            }
            return bytes.toString(Charsets.UTF_8)
        }
    }

    private fun looksBinary(bytes: ByteArray): Boolean {
        val sample = minOf(bytes.size, 8192)
        for (i in 0 until sample) if (bytes[i].toInt() == 0) return true
        return false
    }

    companion object {
        private const val DEFAULT_IO_BUFFER_SIZE = 32 * 1024
        private const val MAX_EDITOR_FILE_BYTES = 10 * 1024 * 1024
        private const val SAF_LOG_TAG = "ProjectFilesystem"
    }


    fun deleteProjects(projectIds: Set<String>) {
        val projectsToDelete = _projects.value.filter { it.id in projectIds }
        projectsToDelete.forEach { proj ->
            if (proj.sourceUri == null) {
                try {
                    ProjectTreeOperations.projectRootDir(proj, storageRootDir)?.deleteRecursively()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        _projects.update { current -> current.filterNot { it.id in projectIds } }
        persistProjectsIndex()
        if (_activeProject.value?.id in projectIds) {
            val remaining = _projects.value.firstOrNull()
            _activeProject.value = remaining
            _openTabs.value = emptyList()
            _activeTabId.value = null
            remaining?.let { selectProject(it) }
        }
    }

    fun saveActiveProject() {
        val project = _activeProject.value ?: return
        autoSaveDirtyTabs()
        val updated = _activeProject.value ?: project
        projectOpenSnapshots[updated.id] = ProjectTreeOperations.deepCopyProject(updated)
        persistProjectsIndex()
    }

    fun closeActiveProjectWithoutSaving() {
        _openTabs.value = emptyList()
        _activeTabId.value = null
        _activeProject.value = null
    }

    fun renameFile(fileId: String, newName: String) {
        val sourceProj = _activeProject.value ?: return
        val oldItem = ProjectTreeOperations.findNodeById(sourceProj.files, fileId) ?: return
        val proj = ProjectTreeOperations.deepCopyProject(sourceProj)
        val trimmedName = newName.trim()
        if (trimmedName.isBlank() || oldItem.path == proj.name || trimmedName == oldItem.name) return

        if (proj.sourceUri != null) {
            val uri = oldItem.sourceUri?.let(Uri::parse) ?: return
            val context = appContext ?: return
            val renamed = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.renameTo(trimmedName) == true
            if (!renamed) return
        } else {
            val root = ProjectTreeOperations.projectRootDir(proj, storageRootDir) ?: return
            val oldDisk = File(root, oldItem.path.removePrefix("${proj.name}/"))
            val parentPath = oldItem.path.substringBeforeLast('/', "")
            val newPath = if (parentPath.isBlank()) trimmedName else "$parentPath/$trimmedName"
            val newDisk = File(root, newPath.removePrefix("${proj.name}/"))
            val parent = newDisk.parentFile ?: return
            if (!oldDisk.exists()) return
            if (!parent.exists() && !parent.mkdirs() && !parent.exists()) return
            if (!oldDisk.renameTo(newDisk)) return
        }

        val oldPath = oldItem.path
        val type = ProjectTreeOperations.detectFileType(trimmedName, oldItem.isDirectory)
        val newPath = if (oldPath.contains('/')) oldPath.substringBeforeLast('/') + "/" + trimmedName else trimmedName
        if (!ProjectTreeOperations.renameNodeInTree(proj.files, fileId, trimmedName, type)) return
        ProjectTreeOperations.updateDescendantPaths(proj.files, fileId)

        _openTabs.update { tabs ->
            tabs.map { tab ->
                when {
                    tab.fileId == fileId -> tab.copy(fileName = trimmedName, filePath = newPath, fileType = type)
                    oldItem.isDirectory && tab.filePath.startsWith("$oldPath/") -> tab.copy(filePath = newPath + tab.filePath.removePrefix(oldPath))
                    else -> tab
                }
            }
        }
        _activeProject.value = proj.copy()
        persistProjectsIndex()
    }

    fun deleteFile(fileId: String) {
        val sourceProj = _activeProject.value ?: return
        val target = ProjectTreeOperations.findNodeById(sourceProj.files, fileId) ?: return
        val proj = ProjectTreeOperations.deepCopyProject(sourceProj)
        if (target.path == proj.name) return

        if (proj.sourceUri != null) {
            val uri = target.sourceUri?.let(Uri::parse) ?: return
            val context = appContext ?: return
            if (androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)?.delete() != true) return
        } else {
            val root = ProjectTreeOperations.projectRootDir(proj, storageRootDir) ?: return
            if (!File(root, target.path.removePrefix("${proj.name}/")).deleteRecursively()) return
        }

        val deletedPath = target.path
        if (!ProjectTreeOperations.deleteNodeFromTree(proj.files, fileId)) return
        _openTabs.update { tabs ->
            tabs.filterNot { tab ->
                tab.fileId == fileId || (target.isDirectory && tab.filePath.startsWith("$deletedPath/"))
            }
        }
        if (_activeTabId.value == fileId || _openTabs.value.none { it.fileId == _activeTabId.value }) {
            _activeTabId.value = _openTabs.value.lastOrNull()?.fileId
        }
        _activeProject.value = proj.copy()
        persistProjectsIndex()
    }

    suspend fun openFile(file: FileItem): Result<Unit> {
        if (file.isDirectory) return Result.failure(IllegalArgumentException("Pasta não pode ser aberta no editor"))
        if (_openTabs.value.any { it.fileId == file.id }) {
            _activeTabId.value = file.id
            return Result.success(Unit)
        }
        val project = _activeProject.value ?: return Result.failure(IllegalStateException("Nenhum projeto aberto"))
        val context = appContext ?: return Result.failure(IllegalStateException("Contexto do aplicativo não inicializado"))
        android.util.Log.d(SAF_LOG_TAG, "Abrindo arquivo '${file.path}' (uri=${file.sourceUri})")
        return runCatching {
            val content = withContext(Dispatchers.IO) { readFileContent(context, project, file) }
            val newTab = EditorTab(
                fileId = file.id,
                fileName = file.name,
                filePath = file.path,
                fileType = file.fileType,
                content = content
            )
            _openTabs.update { tabs -> tabs + newTab }
            _activeTabId.value = file.id
        }
    }

    fun closeTab(fileId: String) {
        val currentTabs = _openTabs.value.toMutableList()
        val index = currentTabs.indexOfFirst { it.fileId == fileId }
        if (index < 0) return

        val tab = currentTabs[index]
        if (tab.isDirty) {
            val project = _activeProject.value
            if (project != null) {
                ProjectTreeOperations.updateFileContentInTree(project.files, tab.fileId, tab.content)
                persistFileToDisk(project.name, tab.filePath, tab.content)
                _activeProject.value = project.copy()
            }
        }

        currentTabs.removeAt(index)
        _openTabs.value = currentTabs
        if (_activeTabId.value == fileId) {
            _activeTabId.value = currentTabs.lastOrNull()?.fileId
        }
    }

    fun selectTab(fileId: String) {
        _activeTabId.value = fileId
    }

    fun updateActiveTabContent(newContent: String) {
        val activeId = _activeTabId.value ?: return
        _openTabs.update { tabs ->
            tabs.map { tab ->
                if (tab.fileId == activeId) {
                    tab.copy(content = newContent, isDirty = true)
                } else tab
            }
        }
    }

    fun saveActiveTab() {
        val activeId = _activeTabId.value ?: return
        val currentTab = _openTabs.value.find { it.fileId == activeId } ?: return
        val sourceProj = _activeProject.value ?: return
        val proj = ProjectTreeOperations.deepCopyProject(sourceProj)

        ProjectTreeOperations.updateFileContentInTree(proj.files, activeId, currentTab.content)
        val saved = persistFileToDisk(proj.name, currentTab.filePath, currentTab.content)

        if (saved) {
            _openTabs.update { tabs ->
                tabs.map { if (it.fileId == activeId) it.copy(isDirty = false) else it }
            }
            _activeProject.value = proj.copy()
        }
    }

    suspend fun toggleFolderExpanded(fileId: String): Result<Unit> {
        if (fileId in _loadingNodeIds.value) return Result.success(Unit)

        val sourceProject = _activeProject.value
            ?: return Result.failure(IllegalStateException("Nenhum projeto aberto"))
        val currentFolder = ProjectTreeOperations.findNodeById(sourceProject.files, fileId)
            ?: return Result.failure(IllegalArgumentException("Pasta não encontrada"))
        if (!currentFolder.isDirectory) {
            return Result.failure(IllegalArgumentException("O item não é uma pasta"))
        }

        if (currentFolder.isExpanded) {
            val collapsed = ProjectTreeOperations.deepCopyProject(sourceProject)
            ProjectTreeOperations.replaceNode(collapsed.files, fileId) { it.copy(isExpanded = false) }
            _activeProject.value = collapsed
            return Result.success(Unit)
        }

        _loadingNodeIds.update { it + fileId }
        android.util.Log.d(SAF_LOG_TAG, "Expandindo pasta '${currentFolder.path}' (uri=${currentFolder.sourceUri})")
        return try {
            // Every expansion refreshes that directory from the real project.
            // This makes files/folders created outside the app appear without
            // requiring a project re-import, while the I/O stays off the UI thread.
            val children = withContext(Dispatchers.IO) {
                loadDirectoryChildren(sourceProject, currentFolder)
            }
            val latestProject = _activeProject.value ?: sourceProject
            val updated = ProjectTreeOperations.deepCopyProject(latestProject)
            ProjectTreeOperations.replaceNode(updated.files, fileId) { node ->
                // Preserva os ids e o estado (expandido/carregado) dos filhos que já
                // existiam antes deste refresh, em vez de gerar ids novos (FileItem()
                // usa UUID aleatório por padrão) a cada vez que a pasta é reaberta.
                // Sem isso, uma sub-pasta previamente expandida perdia seu estado e o
                // id usado em fechamentos de clique (onClick) na árvore ficava
                // desatualizado em relação ao id real na árvore — fazendo o toque em
                // um arquivo/pasta buscado ou já visitado silenciosamente falhar.
                node.copy(
                    isExpanded = true,
                    children = mergeDirectoryChildren(node.children, children),
                    childrenLoaded = true
                )
            }
            _activeProject.value = updated
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(SAF_LOG_TAG, "Falha ao expandir pasta '${currentFolder.path}'", e)
            Result.failure(e)
        } finally {
            _loadingNodeIds.update { it - fileId }
        }
    }

    

    

    

    

    

    

    fun addFileToActiveProject(parentFolderId: String?, fileName: String, isDir: Boolean): FileItem? {
        val sourceProj = _activeProject.value ?: return null
        val proj = ProjectTreeOperations.deepCopyProject(sourceProj)
        val cleanName = fileName.trim()
        if (cleanName.isBlank() || cleanName.contains('/') || cleanName.contains('\\')) return null
        val parent = parentFolderId?.let { ProjectTreeOperations.findNodeById(proj.files, it) }
            ?: proj.files.firstOrNull { it.isDirectory }
        val parentPath = parent?.path ?: proj.name

        if (proj.sourceUri != null) {
            val context = appContext ?: return null
            val parentUri = parent?.sourceUri?.let(Uri::parse) ?: proj.sourceUri.let(Uri::parse)
            val parentDoc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, parentUri) ?: return null
            val created = if (isDir) parentDoc.createDirectory(cleanName) else {
                val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(cleanName.substringAfterLast('.', "").lowercase()) ?: "text/plain"
                parentDoc.createFile(mime, cleanName)
            } ?: return null
            val newFile = FileItem(
                name = cleanName,
                path = "$parentPath/$cleanName",
                isDirectory = isDir,
                fileType = ProjectTreeOperations.detectFileType(cleanName, isDir),
                sourceUri = created.uri.toString(),
                childrenLoaded = !isDir
            )
            if (parent != null) ProjectTreeOperations.addNodeToTree(proj.files, parent.id, newFile) else proj.files.add(newFile)
            _activeProject.value = proj.copy()
            persistProjectsIndex()
            return newFile
        }

        val type = ProjectTreeOperations.detectFileType(cleanName, isDir)
        val newPath = "$parentPath/$cleanName"
        val newFile = FileItem(
            name = cleanName,
            path = newPath,
            isDirectory = isDir,
            fileType = type,
            content = "",
            childrenLoaded = !isDir
        )
        if (parentFolderId == null) proj.files.firstOrNull()?.children?.add(newFile) ?: proj.files.add(newFile)
        else if (!ProjectTreeOperations.addNodeToTree(proj.files, parentFolderId, newFile)) return null

        val root = ProjectTreeOperations.projectRootDir(proj, storageRootDir) ?: return null
        val disk = File(root, newPath.removePrefix("${proj.name}/"))
        if (isDir) disk.mkdirs() else {
            disk.parentFile?.mkdirs()
            disk.createNewFile()
        }
        _activeProject.value = proj.copy()
        persistProjectsIndex()
        return newFile
    }

    

    

    


    

    

    

    private fun createDefaultProject(
        name: String,
        language: String = "",
        packageName: String = "com.codeoneditor.app.${name.lowercase().filter { it.isLetterOrDigit() }}",
        createdAt: String = "",
        minSdk: Int = 24
    ): ProjectItem {
        val root = FileItem(
            name = name,
            path = name,
            isDirectory = true,
            isExpanded = true,
            children = mutableListOf(),
            childrenLoaded = true
        )
        return ProjectItem(
            name = name,
            packageName = packageName,
            createdAt = createdAt,
                        language = language,
            minSdk = minSdk,
            files = mutableListOf(root),
            rootPath = storageRootDir?.let { File(it, name).absolutePath }
        )
    }
}
