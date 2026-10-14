package com.codeoneditor.app.editor.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeoneditor.app.data.model.FileItem
import com.codeoneditor.app.data.model.ProjectItem
import com.codeoneditor.app.presentation.MainViewModel
import com.codeoneditor.app.ui.theme.HighDensityBg
import com.codeoneditor.app.ui.theme.HighDensityBorder
import com.codeoneditor.app.ui.theme.HighDensitySurface
import com.codeoneditor.app.ui.theme.HighDensityTextPrimary
import com.codeoneditor.app.ui.theme.HighDensityTextSecondary

private val TreeIndentBase = 18.dp
private val RowHeightBase = 32.dp
private val ActivityBarWidth = 44.dp
private const val MinTreeScale = 0.75f
private const val MaxTreeScale = 1.6f

private data class VisibleNode(
    val item: FileItem,
    val level: Int,
    /** For each connector depth, whether its vertical stem continues below this row. */
    val guideContinues: List<Boolean> = emptyList()
)

private enum class CreateKind { FILE, FOLDER }

@Composable
fun FileTreeSidebar(
    viewModel: MainViewModel,
    onRequestOpenProject: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp
) {
    val project by viewModel.activeProject.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val loadingNodeIds by viewModel.loadingNodeIds.collectAsState()

    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var createDialogKind by remember { mutableStateOf<CreateKind?>(null) }
    // Escala visual somente da árvore (pinch).
    var treeScale by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(HighDensityBg)
    ) {
        SidebarActivityBar()

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(HighDensityBorder)
        )

        FileTreePanel(
            project = project,
            activeFileId = activeTabId,
            loadingNodeIds = loadingNodeIds,
            showSearchField = searchOpen,
            onToggleSearch = { searchOpen = !searchOpen; if (!searchOpen) searchQuery = "" },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onOpenFile = viewModel::openFile,
            onToggleFolder = { file -> viewModel.toggleFolderExpanded(file.id) },
            onRequestCreate = { kind -> createDialogKind = kind },
            onRequestOpenProject = onRequestOpenProject,
            treeScale = treeScale,
            onTreeScaleChange = { newScale ->
                treeScale = newScale.coerceIn(MinTreeScale, MaxTreeScale)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }

    val kind = createDialogKind
    if (kind != null) {
        CreateEntryDialog(
            kind = kind,
            onDismiss = { createDialogKind = null },
            onConfirm = { name ->
                viewModel.addFileToActiveProject(null, name, kind == CreateKind.FOLDER)
                createDialogKind = null
            }
        )
    }
}

@Composable
private fun SidebarActivityBar() {
    Column(
        modifier = Modifier
            .width(ActivityBarWidth)
            .fillMaxHeight()
            .background(HighDensityBg)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
    }
}

@Composable
private fun FileTreePanel(
    project: ProjectItem?,
    activeFileId: String?,
    loadingNodeIds: Set<String>,
    showSearchField: Boolean,
    onToggleSearch: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onToggleFolder: (FileItem) -> Unit,
    onRequestCreate: (CreateKind) -> Unit,
    onRequestOpenProject: () -> Unit,
    treeScale: Float,
    onTreeScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveQuery = if (showSearchField) searchQuery else ""
    val visibleNodes = buildVisibleNodes(project?.files.orEmpty(), effectiveQuery)
    var headerMenuExpanded by remember { mutableStateOf(false) }

    val indent = TreeIndentBase * treeScale
    val rowHeight = RowHeightBase * treeScale
    val iconSize = (16 * treeScale).dp
    val textSize = (13 * treeScale).sp
    val headerIconSize = (18 * treeScale).dp
    val headerTextSize = (15 * treeScale).sp

    Column(
        modifier = modifier
            .background(HighDensityBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        onTreeScaleChange(treeScale * zoom)
                    }
                }
            }
    ) {
        // Cabeçalho compacto: ícone pasta + nome do projeto + menu "..."
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((44 * treeScale).dp.coerceAtLeast(36.dp))
                .background(HighDensityBg)
                .padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = HighDensityTextPrimary,
                modifier = Modifier.size(headerIconSize)
            )
            Spacer(Modifier.width(8.dp))
            // Nome do projeto já aparece na barra superior — aqui só o rótulo
            // da seção, para não repetir o mesmo texto duas vezes na tela.
            Text(
                text = "Arquivos",
                color = HighDensityTextPrimary,
                fontSize = headerTextSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (project != null) {
                IconButton(onClick = onToggleSearch, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = if (showSearchField) "Fechar pesquisa" else "Pesquisar arquivos",
                        tint = if (showSearchField) HighDensityTextPrimary else HighDensityTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box {
                    IconButton(onClick = { headerMenuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Novo arquivo ou pasta",
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = headerMenuExpanded, onDismissRequest = { headerMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Novo arquivo") },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            onClick = { headerMenuExpanded = false; onRequestCreate(CreateKind.FILE) }
                        )
                        DropdownMenuItem(
                            text = { Text("Nova pasta") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            onClick = { headerMenuExpanded = false; onRequestCreate(CreateKind.FOLDER) }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HighDensityBorder)
        )

        if (showSearchField) {
            TreeSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (project == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = HighDensityTextSecondary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text("Nenhum projeto aberto", color = HighDensityTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRequestOpenProject) {
                    Text("Abrir pasta")
                }
            }
        } else if (visibleNodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (effectiveQuery.isBlank()) "Pasta vazia" else "Nenhum resultado",
                    color = HighDensityTextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(visibleNodes, key = { it.item.id }) { node ->
                    FileTreeRow(
                        item = node.item,
                        level = node.level,
                        guideContinues = node.guideContinues,
                        selected = !node.item.isDirectory && node.item.id == activeFileId,
                        loading = node.item.id in loadingNodeIds,
                        indent = indent,
                        rowHeight = rowHeight,
                        iconSize = iconSize,
                        textSize = textSize,
                        onClick = {
                            if (node.item.isDirectory) onToggleFolder(node.item)
                            else onOpenFile(node.item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .height(32.dp)
            .background(HighDensitySurface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = HighDensityTextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Pesquisar arquivos", color = HighDensityTextSecondary, fontSize = 13.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = HighDensityTextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(HighDensityTextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CreateEntryDialog(
    kind: CreateKind,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == CreateKind.FOLDER) "Nova pasta" else "Novo arquivo") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(if (kind == CreateKind.FOLDER) "nome-da-pasta" else "arquivo.txt") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun buildVisibleNodes(items: List<FileItem>, query: String): List<VisibleNode> {
    val flattened = ArrayList<Pair<FileItem, Int>>()

    fun visit(nodes: List<FileItem>, level: Int) {
        for (item in nodes) {
            val matches = query.isBlank() || item.name.contains(query, ignoreCase = true)
            if (matches) flattened += item to level

            if (item.isDirectory && item.isExpanded && item.childrenLoaded) {
                visit(item.children, level + 1)
            }
        }
    }

    visit(items, level = 0)

    return flattened.mapIndexed { index, (item, level) ->
        val guideContinues = if (level == 0) {
            emptyList()
        } else {
            List(level) { depth ->
                val requiredLevel = depth + 1
                var continues = false
                for (nextIndex in index + 1 until flattened.size) {
                    val nextLevel = flattened[nextIndex].second
                    if (nextLevel < requiredLevel) break
                    if (nextLevel >= requiredLevel) {
                        continues = true
                        break
                    }
                }
                continues
            }
        }

        VisibleNode(
            item = item,
            level = level,
            guideContinues = guideContinues
        )
    }
}

@Composable
private fun FileTreeRow(
    item: FileItem,
    level: Int,
    guideContinues: List<Boolean>,
    selected: Boolean,
    loading: Boolean,
    indent: Dp,
    rowHeight: Dp,
    iconSize: Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(if (selected) HighDensitySurface else HighDensityBg)
            .clickable(onClick = onClick)
    ) {
        Spacer(
            modifier = TreeGuides(
                level = level,
                indent = indent,
                guideContinues = guideContinues,
                modifier = Modifier.matchParentSize()
            )
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp + indent * level, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sem setas decorativas — referência visual usa só ícone de pasta/arquivo.
            // Expand/collapse continua no clique na linha inteira.
            if (loading && item.isDirectory) {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize * 0.9f),
                    strokeWidth = 1.5.dp,
                    color = HighDensityTextSecondary
                )
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                tint = HighDensityTextSecondary,
                modifier = Modifier.size(iconSize)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.name,
                color = if (selected) HighDensityTextPrimary else HighDensityTextSecondary,
                fontSize = textSize,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TreeGuides(
    level: Int,
    indent: Dp,
    guideContinues: List<Boolean>,
    modifier: Modifier = Modifier
): Modifier {
    if (level == 0) return modifier

    // The tree guides must remain clearly visible on the dark explorer background.
    // Use the stronger border tone at full opacity and a slightly heavier stroke.
    val guideColor = HighDensityTextSecondary.copy(alpha = 1f)
    val strokeWidth = 2.4.dp

    return modifier.drawBehind {
        val indentPx = indent.toPx()
        val strokePx = strokeWidth.toPx()
        val centerY = size.height / 2f
        val leftPaddingPx = 10.dp.toPx()
        val radius = (indentPx * 0.42f).coerceIn(5.dp.toPx(), 10.dp.toPx())

        // Draw the vertical stems for ancestor levels. A stem is continuous
        // only while that branch still has a visible node below this row.
        for (depth in 0 until level - 1) {
            if (guideContinues.getOrElse(depth) { false }) {
                val x = leftPaddingPx + indentPx * (depth + 0.5f)
                drawLine(
                    color = guideColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }

        // Current branch: a vertical "rebar" stem with a rounded 90-degree
        // elbow into the file/folder icon. The lower segment exists only when
        // another visible sibling continues this branch below the current row.
        val stemX = leftPaddingPx + indentPx * (level - 0.5f)
        val itemX = leftPaddingPx + indentPx * level
        val elbowStartY = centerY - radius
        val elbowEndX = itemX

        drawLine(
            color = guideColor,
            start = androidx.compose.ui.geometry.Offset(stemX, 0f),
            end = androidx.compose.ui.geometry.Offset(stemX, elbowStartY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        val elbow = Path().apply {
            moveTo(stemX, elbowStartY)
            cubicTo(
                stemX,
                centerY - radius * 0.35f,
                stemX + radius * 0.35f,
                centerY,
                stemX + radius,
                centerY
            )
            lineTo(elbowEndX, centerY)
        }

        drawPath(
            path = elbow,
            color = guideColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        if (guideContinues.getOrElse(level - 1) { false }) {
            drawLine(
                color = guideColor,
                start = androidx.compose.ui.geometry.Offset(stemX, centerY),
                end = androidx.compose.ui.geometry.Offset(stemX, size.height),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}
