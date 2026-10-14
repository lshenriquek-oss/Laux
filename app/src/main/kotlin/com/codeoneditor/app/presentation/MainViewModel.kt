package com.codeoneditor.app.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codeoneditor.app.data.model.AppSettingsState
import com.codeoneditor.app.data.model.FileItem
import com.codeoneditor.app.data.model.ProjectItem
import com.codeoneditor.app.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado e navegação da interface principal do Code Editor.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository()

    val projects = repository.projects
    val settings: StateFlow<AppSettingsState> = repository.settings
    val activeProject = repository.activeProject
    val openTabs = repository.openTabs
    val activeTabId = repository.activeTabId
    val loadingNodeIds = repository.loadingNodeIds

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _comingSoonBanner = MutableStateFlow(false)
    val comingSoonBanner: StateFlow<Boolean> = _comingSoonBanner.asStateFlow()

    private val _projectSearchQuery = MutableStateFlow("")
    val projectSearchQuery: StateFlow<String> = _projectSearchQuery.asStateFlow()

    private val _selectedForDeletion = MutableStateFlow<Set<String>>(emptySet())
    val selectedForDeletion: StateFlow<Set<String>> = _selectedForDeletion.asStateFlow()

    private val _isCloningRepo = MutableStateFlow(false)
    val isCloningRepo: StateFlow<Boolean> = _isCloningRepo.asStateFlow()

    private val _isOpeningProject = MutableStateFlow(false)
    val isOpeningProject: StateFlow<Boolean> = _isOpeningProject.asStateFlow()

    init {
        repository.loadPersistedSettings(application)
        viewModelScope.launch(Dispatchers.IO) {
            repository.initStorage(application.filesDir)
            repository.restoreLastProjectIfEnabled()
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setProjectSearchQuery(query: String) {
        _projectSearchQuery.value = query
    }

    fun requestProjectCreation(
        name: String,
        packageName: String,
        language: String,
        saveLocation: String
    ) {
        try {
            repository.setStorageRoot(saveLocation)
            repository.createProject(
                name = name,
                packageName = packageName.ifBlank {
                    "com.codeoneditor.app.${name.lowercase().filter { it.isLetterOrDigit() }}"
                },
                language = language
            )
            // Abre diretamente a tela do Editor.
        } catch (e: Exception) {
            showToast("Não foi possível criar o projeto")
        }
    }

    fun selectProject(project: ProjectItem) {
        if (_isOpeningProject.value) return
        _isOpeningProject.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.openProject(project)
            withContext(Dispatchers.Main) {
                _isOpeningProject.value = false
                result.onSuccess { refreshed ->
                    val firstFile = findFirstFile(refreshed.files)
                    if (firstFile != null) openFile(firstFile)
                }.onFailure { error ->
                    showToast(error.message ?: "Não foi possível abrir o projeto")
                }
            }
        }
    }

    private fun findFirstFile(items: List<FileItem>): FileItem? {
        for (item in items) {
            if (!item.isDirectory) return item
            findFirstFile(item.children)?.let { return it }
        }
        return null
    }

    fun importProjectFromFolder(uri: Uri) {
        if (_isOpeningProject.value) return
        _isOpeningProject.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val imported = runCatching {
                repository.importProjectFromTree(getApplication(), uri)
            }.getOrNull()

            withContext(Dispatchers.Main) {
                _isOpeningProject.value = false
                if (imported != null) {
                    // Enter the editor immediately. The potentially expensive
                    // private backup continues in the background.
                    val firstFile = findFirstFile(imported.files)
                    if (firstFile != null) openFile(firstFile)
                    viewModelScope.launch(Dispatchers.IO) {
                        repository.backupImportedProject(imported)
                    }
                } else {
                    showToast("Não foi possível abrir a pasta selecionada")
                }
            }
        }
    }

    /**
     * Clona um repositório Git (hospedado no GitHub) e o importa como um
     * novo projeto. Roda em background (Dispatchers.IO) para não travar a
     * UI durante o download da rede.
     */
    fun cloneGitRepository(
        repoUrl: String,
        branch: String,
        saveLocation: String,
        username: String = "",
        token: String = ""
    ) {
        if (_isCloningRepo.value) return
        val url = repoUrl.trim()
        if (url.isBlank()) {
            showToast("Informe a URL do repositório")
            return
        }
        _isCloningRepo.value = true
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                repository.cloneRepository(
                    context = getApplication(),
                    repoUrl = url,
                    branch = branch.trim(),
                    saveLocation = saveLocation,
                    username = username.trim(),
                    token = token.trim()
                )
            }
            _isCloningRepo.value = false
            if (imported != null) {
            } else {
                showToast("Não foi possível clonar o repositório. Verifique a URL, o branch e a conexão.")
            }
        }
    }

    fun toggleProjectSelection(projectId: String) {
        _selectedForDeletion.value = _selectedForDeletion.value.toMutableSet().apply {
            if (!add(projectId)) remove(projectId)
        }
    }

    fun deleteSelectedProjects() {
        val selected = _selectedForDeletion.value
        if (selected.isEmpty()) {
            showToast("Selecione pelo menos um projeto para excluir")
            return
        }
        repository.deleteProjects(selected)
        _selectedForDeletion.value = emptySet()
        showToast("Projeto(s) excluído(s)")
    }


    fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        repository.updateSettings(transform)
    }

    fun exportActiveProject(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.exportActiveProject(getApplication(), uri)
            withContext(Dispatchers.Main) {
                showToast(
                    if (success) "Projeto exportado com sucesso"
                    else "Não foi possível exportar o projeto"
                )
            }
        }
    }


    fun updateActiveTabContent(newContent: String) {
        repository.updateActiveTabContent(newContent)
    }

    fun saveActiveTab() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveActiveTab()
        }
    }

    /** Editor actions are exposed here so the UI never reaches into the repository directly. */
    fun openFile(file: FileItem) {
        viewModelScope.launch {
            repository.openFile(file).onFailure { error ->
                showToast(error.message ?: "Não foi possível abrir o arquivo")
            }
        }
    }

    fun toggleFolderExpanded(fileId: String) {
        viewModelScope.launch {
            repository.toggleFolderExpanded(fileId).onFailure { error ->
                showToast(error.message ?: "Não foi possível abrir a pasta")
            }
        }
    }

    fun selectTab(fileId: String) {
        repository.selectTab(fileId)
    }

    fun closeTab(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.closeTab(fileId)
        }
    }

    fun addFileToActiveProject(parentFolderId: String?, fileName: String, isDir: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val created = repository.addFileToActiveProject(parentFolderId, fileName, isDir)
            if (created != null && !isDir) {
                repository.openFile(created).onFailure { error ->
                    withContext(Dispatchers.Main) {
                        showToast(error.message ?: "Não foi possível abrir o arquivo")
                    }
                }
            }
        }
    }

    fun renameFile(fileId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.renameFile(fileId, newName) }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteFile(fileId) }
    }
}
