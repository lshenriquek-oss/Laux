package com.codeoneditor.app.editor.screen

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.codeoneditor.app.presentation.MainViewModel
import com.codeoneditor.core.editor.components.CodeEditorPane
import com.codeoneditor.core.editor.components.EditorTabs
import com.codeoneditor.core.editor.components.EditorTopBar
import com.codeoneditor.app.editor.tree.FileTreeSidebar
import com.codeoneditor.app.ui.theme.HighDensityBg
import com.codeoneditor.app.ui.theme.HighDensityTextPrimary

/**
 * Main editor workspace: application UI owns navigation and the real project tree,
 * while editor-core owns reusable editing and syntax components.
 */
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.openTabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()

    var isExplorerOpen by remember { mutableStateOf(false) }
    val activeTab = tabs.firstOrNull { it.fileId == activeTabId }
    val context = LocalContext.current
    val openProjectLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.importProjectFromFolder(uri)
    }

    BackHandler {
        if (isExplorerOpen) isExplorerOpen = false
        else (context as? Activity)?.finish()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("editor_screen")
    ) {
        EditorTopBar(
            canSave = activeTab?.isDirty == true,
            onToggleExplorer = { isExplorerOpen = !isExplorerOpen },
            onSave = viewModel::saveActiveTab
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val minPanelWidth = 220.dp.coerceAtMost((maxWidth - 56.dp).coerceAtLeast(0.dp))
            val maxPanelWidth = (maxWidth - 56.dp).coerceAtLeast(minPanelWidth)
            var panelWidth by remember(maxWidth) {
                mutableStateOf(
                    (maxWidth * 0.82f)
                        .coerceAtMost(300.dp)
                        .coerceIn(minPanelWidth, maxPanelWidth)
                )
            }

            // Do not animate this value while resizing. Re-starting a Dp animation on every
            // drag event makes the file tree feel laggy. The panel follows the finger directly.
            val panelOffset = if (isExplorerOpen) 0.dp else -panelWidth
            val scrimAlpha by animateFloatAsState(
                targetValue = if (isExplorerOpen) 0.45f else 0f,
                label = "project_explorer_scrim"
            )
            val scrimInteractionSource = remember { MutableInteractionSource() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (tabs.isNotEmpty()) {
                    EditorTabs(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelect = viewModel::selectTab,
                        onClose = viewModel::closeTab
                    )
                }

                CodeEditorPane(
                    tab = activeTab,
                    onContentChange = viewModel::updateActiveTabContent,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .then(
                        if (isExplorerOpen) {
                            Modifier.clickable(
                                indication = null,
                                interactionSource = scrimInteractionSource
                            ) { isExplorerOpen = false }
                        } else Modifier
                    )
            )

            // Drawer gestures are restricted to the screen edge so they cannot
            // steal horizontal swipes from the code editor's horizontalScroll.
            if (!isExplorerOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(28.dp)
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    totalDrag += amount
                                },
                                onDragEnd = {
                                    if (totalDrag > 56f) isExplorerOpen = true
                                    totalDrag = 0f
                                },
                                onDragCancel = { totalDrag = 0f }
                            )
                        }
                )
            }

            FileTreeSidebar(
                viewModel = viewModel,
                onRequestOpenProject = { openProjectLauncher.launch(null) },
                width = panelWidth,
                modifier = Modifier
                    .fillMaxHeight()
                    .offset(x = panelOffset)
            )

            if (isExplorerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(28.dp)
                        .offset(x = panelWidth - 28.dp)
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    totalDrag += amount
                                },
                                onDragEnd = {
                                    if (totalDrag < -56f) isExplorerOpen = false
                                    totalDrag = 0f
                                },
                                onDragCancel = { totalDrag = 0f }
                            )
                        }
                )
            }

            if (isExplorerOpen) {
                val resizeState = rememberDraggablePanelState(
                    minWidth = minPanelWidth,
                    maxWidth = maxPanelWidth,
                    currentWidth = panelWidth,
                    onWidthChange = { panelWidth = it }
                )

                // Compact resize grip fixed directly on the panel edge.
                // The whole grip is the touch target; the dots are only visual.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = panelOffset + panelWidth - 12.dp)
                        .width(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ResizeGrip(resizeState)
                }
            }
        }
    }
}

private class DraggablePanelState(
    val minWidth: androidx.compose.ui.unit.Dp,
    val maxWidth: androidx.compose.ui.unit.Dp,
    val currentWidth: androidx.compose.ui.unit.Dp,
    val onWidthChange: (androidx.compose.ui.unit.Dp) -> Unit
)

@Composable
private fun rememberDraggablePanelState(
    minWidth: androidx.compose.ui.unit.Dp,
    maxWidth: androidx.compose.ui.unit.Dp,
    currentWidth: androidx.compose.ui.unit.Dp,
    onWidthChange: (androidx.compose.ui.unit.Dp) -> Unit
): DraggablePanelState = remember(minWidth, maxWidth, currentWidth, onWidthChange) {
    DraggablePanelState(minWidth, maxWidth, currentWidth, onWidthChange)
}

@Composable
private fun ResizeGrip(state: DraggablePanelState) {
    val currentWidth by rememberUpdatedState(state.currentWidth)
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .size(width = 20.dp, height = 52.dp)
            .background(HighDensityBg, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .pointerInput(state.minWidth, state.maxWidth) {
                var dragWidth = currentWidth
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragWidth = (dragWidth + with(density) { amount.toDp() })
                            .coerceIn(state.minWidth, state.maxWidth)
                        state.onWidthChange(dragWidth)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(HighDensityTextPrimary.copy(alpha = 0.72f), CircleShape)
                )
            }
        }
    }
}
