package com.codeoneditor.core.editor.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.layout.widthIn
import com.codeoneditor.core.editor.model.EditorDocument
import com.codeoneditor.core.editor.model.EditorTab
import com.codeoneditor.core.editor.EditorEngine
import com.codeoneditor.core.editor.ui.EditorColors
import com.codeoneditor.core.editor.syntax.CodeSyntaxHighlighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EditorTopBar(
    canSave: Boolean,
    onToggleExplorer: () -> Unit,
    onSave: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Sem nome/título de projeto na barra — só os controles de navegação e
    // ações do arquivo. A lista de arquivos continua no painel lateral.
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(EditorColors.background)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleExplorer, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Abrir explorador de arquivos",
                    tint = EditorColors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mais opções",
                        tint = EditorColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Salvar") },
                        enabled = canSave,
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                        onClick = { menuExpanded = false; onSave() }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EditorColors.border)
        )
    }
}

@Composable
fun EditorTabs(
    tabs: List<EditorTab>,
    activeTabId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(EditorColors.background)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val active = tab.fileId == activeTabId
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .widthIn(min = 112.dp, max = 220.dp)
                    .background(if (active) EditorColors.surface else EditorColors.background)
                    .clickable { onSelect(tab.fileId) }
                    .padding(start = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = EditorColors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = tab.fileName,
                    color = if (active) EditorColors.textPrimary else EditorColors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (tab.isDirty) {
                    Text("•", color = EditorColors.textPrimary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 3.dp))
                }
                IconButton(onClick = { onClose(tab.fileId) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Fechar ${tab.fileName}", tint = EditorColors.textSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyEditorState(projectName: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Estado vazio simples, sem texto tutorial explicando como criar um
        // projeto/arquivo ou como abrir o explorador — o painel de arquivos já
        // abre sozinho quando não há nenhum arquivo ativo.
        Icon(Icons.Default.Folder, null, tint = EditorColors.textSecondary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (projectName == null) "Nenhum projeto aberto" else "Nenhum arquivo aberto",
            color = EditorColors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}



private const val MIN_EDITOR_ZOOM = 0.50f
private const val MAX_EDITOR_ZOOM = 4.00f

/**
 * Base line-number size. It is multiplied by the same zoom factor as the code
 * so the gutter typography stays proportional to the editor text.
 */
private const val LINE_NUMBER_FONT_SIZE_SP = 14f
private val LINE_NUMBER_HORIZONTAL_PADDING = 4.dp
private val LINE_NUMBER_LEFT_PADDING = 2.dp
private val EDITOR_TOP_PADDING = 8.dp
private val EDITOR_BOTTOM_PADDING = 24.dp

internal fun calculateLineNumberFontSizeSp(zoom: Float): Float =
    LINE_NUMBER_FONT_SIZE_SP * clampEditorZoom(zoom)

internal fun calculateLineNumberGutterWidth(numberTextWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
    maxOf(12.dp, numberTextWidth + LINE_NUMBER_HORIZONTAL_PADDING)

/**
 * Keeps the line-number text anchored to the left edge of the editor gutter.
 * This value intentionally does not depend on zoom: zoom changes the glyph size,
 * not its horizontal anchor.
 */
internal fun calculateLineNumberTextLeftPadding(): androidx.compose.ui.unit.Dp =
    LINE_NUMBER_LEFT_PADDING

internal fun calculateLineNumberBaselineY(
    lineIndex: Int,
    lineHeightPx: Float,
    scrollOffsetPx: Float,
    baselineOffsetPx: Float
): Float =
    lineIndex * lineHeightPx - scrollOffsetPx + baselineOffsetPx

@Composable
private fun LineNumberGutter(
    lineCount: Int,
    lineHeight: androidx.compose.ui.unit.Dp,
    gutterWidth: androidx.compose.ui.unit.Dp,
    scrollOffsetPx: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // The gutter remains a separate viewport so it does not horizontally
    // scroll with the code, but its typography uses the same zoomed metrics
    // as the code. This keeps size and vertical alignment proportional.
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .width(gutterWidth)
            .fillMaxHeight()
            .clipToBounds()
    ) {
        val rowHeightPx = lineHeight.toPx()
        val leftPaddingPx = with(density) { calculateLineNumberTextLeftPadding().toPx() }
        val scrollOffset = scrollOffsetPx.toFloat()

        if (rowHeightPx > 0f) {
            val firstVisibleLine =
                (scrollOffset / rowHeightPx).toInt().coerceAtLeast(0) + 1
            val visibleLineCount = (size.height / rowHeightPx).toInt() + 2
            val lastVisibleLine =
                (firstVisibleLine + visibleLineCount).coerceAtMost(lineCount)

            if (firstVisibleLine <= lastVisibleLine) {
                for (line in firstVisibleLine..lastVisibleLine) {
                    val lineText = line.toString()
                    val layout = textMeasurer.measure(
                        text = lineText,
                        style = textStyle,
                        maxLines = 1
                    )
                    val baseline = calculateLineNumberBaselineY(
                        lineIndex = line - 1,
                        lineHeightPx = rowHeightPx,
                        scrollOffsetPx = scrollOffset,
                        baselineOffsetPx = (rowHeightPx - layout.size.height) / 2f + layout.firstBaseline
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = lineText,
                        topLeft = Offset(
                            x = leftPaddingPx,
                            y = baseline - layout.firstBaseline
                        ),
                        style = textStyle,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

internal fun clampEditorZoom(value: Float): Float =
    value.coerceIn(MIN_EDITOR_ZOOM, MAX_EDITOR_ZOOM)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeEditorPane(
    tab: EditorTab?,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tab == null) {
        EmptyEditorState(null)
        return
    }

    val engine = remember(tab.fileId) {
        EditorEngine(
            EditorDocument(
                fileId = tab.fileId,
                fileName = tab.fileName,
                filePath = tab.filePath,
                fileType = tab.fileType,
                content = tab.content
            )
        )
    }

    var textFieldValue by remember(tab.fileId) {
        mutableStateOf(
            TextFieldValue(
                text = tab.content,
                selection = TextRange(tab.content.length)
            )
        )
    }
    var zoom by remember(tab.fileId) { mutableFloatStateOf(1f) }

    val verticalScrollState = remember(tab.fileId) { ScrollState(0) }
    val horizontalScrollState = remember(tab.fileId) { ScrollState(0) }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val text = textFieldValue.text

    // Pinch zoom changes the actual text metrics instead of scaling the whole
    // editor as a bitmap. Code and line numbers use the same zoom factor.
    val fontSize = (14f * zoom).sp
    val lineHeight = (20f * zoom).sp
    val lineHeightDp = with(density) { lineHeight.toDp() }
    val lineCount = maxOf(1, text.count { it == '\n' } + 1)
    val lineNumberDigits = lineCount.toString().length

    val lineNumberTextStyle = androidx.compose.ui.text.TextStyle(
        color = Color(0xFF777777),
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        fontSize = calculateLineNumberFontSizeSp(zoom).sp,
        lineHeight = lineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val lineNumberTextWidth = with(density) {
        textMeasurer.measure(
            text = "8".repeat(lineNumberDigits),
            style = lineNumberTextStyle
        ).size.width.toDp()
    }
    val gutterWidth = calculateLineNumberGutterWidth(lineNumberTextWidth)

    val codeTextStyle = androidx.compose.ui.text.TextStyle(
        color = Color(0xFFEDEDED),
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        fontSize = fontSize,
        lineHeight = lineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    /*
     * Syntax highlighting is deliberately moved off the UI thread. Pasting a
     * large source file used to run the tokenizer and build all AnnotatedString
     * spans synchronously during composition, which could freeze or kill the
     * editor. While the background pass is running, the plain text remains
     * fully editable and visible.
     */
    var highlightedText by remember(tab.fileId) {
        mutableStateOf(
            AnnotatedString(text)
        )
    }

    LaunchedEffect(text, tab.fileType) {
        val source = text
        highlightedText = withContext(Dispatchers.Default) {
            // Extremely large pastes/files are kept plain to avoid creating
            // millions of syntax spans and exhausting the phone's heap.
            if (source.length > 500_000) {
                AnnotatedString(source)
            } else {
                CodeSyntaxHighlighter.highlightCode(source, tab.fileType)
            }
        }
    }

    val maxLineLength = remember(text) {
        text.lineSequence().maxOfOrNull { it.length } ?: 0
    }

    val measuredCharacterWidth = remember(codeTextStyle.fontSize) {
        textMeasurer.measure(
            text = "0",
            style = codeTextStyle
        ).size.width.toFloat()
    }

    LaunchedEffect(tab.fileId, tab.content, tab.isDirty) {
        if (!tab.isDirty && text != tab.content) {
            engine.replaceExternalContent(tab.content)
            textFieldValue = TextFieldValue(
                text = tab.content,
                selection = TextRange(tab.content.length)
            )
        }
    }

    fun commitValue(value: TextFieldValue) {
        val safe = value.copy(
            selection = TextRange(
                value.selection.min.coerceIn(0, value.text.length),
                value.selection.max.coerceIn(0, value.text.length)
            )
        )
        if (safe.text != text) {
            runCatching {
                engine.replaceText(
                    newText = safe.text,
                    cursor = safe.selection.end
                )
                onContentChange(safe.text)
            }.onFailure {
                // Keep the editor alive if an unexpected document operation
                // fails. The current valid buffer is never replaced by garbage.
                return
            }
        }
        textFieldValue = safe
    }

    fun copySelection() {
        val selection = textFieldValue.selection
        if (selection.collapsed) return
        val selected = text.substring(
            selection.min.coerceAtLeast(0),
            selection.max.coerceAtMost(text.length)
        )
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Code", selected)
        )
    }

    fun cutSelection() {
        val selection = textFieldValue.selection
        if (selection.collapsed) return
        copySelection()
        val start = selection.min.coerceAtLeast(0)
        val end = selection.max.coerceAtMost(text.length)
        commitValue(
            TextFieldValue(
                text = text.removeRange(start, end),
                selection = TextRange(start)
            )
        )
    }

    fun pasteClipboard() {
        val clip = clipboard.primaryClip ?: return
        if (clip.itemCount == 0) return
        val pasted = runCatching {
            clip.getItemAt(0).coerceToText(context).toString()
        }.getOrNull() ?: return
        if (pasted.isEmpty()) return

        val selection = textFieldValue.selection
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(start, text.length)

        // Build only the replacement range instead of routing the clipboard
        // operation through a second text widget. This avoids the paste path
        // that was closing the editor.
        val newText = buildString(text.length - (end - start) + pasted.length) {
            append(text, 0, start)
            append(pasted)
            append(text, end, text.length)
        }
        commitValue(
            TextFieldValue(
                text = newText,
                selection = TextRange(start + pasted.length)
            )
        )
    }

    fun selectAllText() {
        textFieldValue = textFieldValue.copy(
            selection = TextRange(0, text.length)
        )
    }

    fun selectCurrentLine() {
        if (text.isEmpty()) return
        val cursor = textFieldValue.selection.end.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', cursor - 1).let { if (it < 0) 0 else it + 1 }
        val nextBreak = text.indexOf('\n', cursor)
        val end = if (nextBreak < 0) text.length else nextBreak
        textFieldValue = textFieldValue.copy(
            selection = TextRange(start, end)
        )
    }

    val zoomGestureModifier = Modifier.pointerInput(tab.fileId) {
        while (true) {
            var pendingHorizontalDelta = 0f
            var pendingVerticalDelta = 0f
            awaitPointerEventScope {
                awaitFirstDown(requireUnconsumed = false)
                var event = awaitPointerEvent()
                var pinching = false
                var currentZoom = zoom

                while (event.changes.any { it.pressed }) {
                    if (event.changes.count { it.pressed } >= 2) {
                        if (!pinching) {
                            pinching = true
                            currentZoom = zoom
                        }

                        val zoomChange = event.calculateZoom()
                        val centroid = event.calculateCentroid()

                        if (zoomChange.isFinite() && zoomChange > 0f &&
                            centroid != Offset.Unspecified
                        ) {
                            val oldZoom = currentZoom
                            val newZoom = clampEditorZoom(oldZoom * zoomChange)

                            if (newZoom != oldZoom) {
                                val factor = newZoom / oldZoom
                                val gutterWidthPx = with(density) { gutterWidth.toPx() }
                                val topPaddingPx = with(density) { EDITOR_TOP_PADDING.toPx() }

                                val codeCentroidX = (centroid.x - gutterWidthPx).coerceAtLeast(0f)
                                val codeCentroidY = (centroid.y - topPaddingPx).coerceAtLeast(0f)
                                val targetHorizontal =
                                    ((horizontalScrollState.value + codeCentroidX) * factor - codeCentroidX)
                                        .coerceAtLeast(0f)
                                val targetVertical =
                                    ((verticalScrollState.value + codeCentroidY) * factor - codeCentroidY)
                                        .coerceAtLeast(0f)

                                currentZoom = newZoom
                                zoom = newZoom
                                pendingHorizontalDelta =
                                    targetHorizontal - horizontalScrollState.value
                                pendingVerticalDelta =
                                    targetVertical - verticalScrollState.value
                            }
                        }

                        event.changes.forEach { change ->
                            change.consume()
                        }
                    }
                    event = awaitPointerEvent()
                }
            }
            if (pendingHorizontalDelta != 0f) {
                horizontalScrollState.dispatchRawDelta(pendingHorizontalDelta)
            }
            if (pendingVerticalDelta != 0f) {
                verticalScrollState.dispatchRawDelta(pendingVerticalDelta)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        val codeViewportWidth = (viewportWidth - gutterWidth).coerceAtLeast(1.dp)
        val measuredTextWidth = with(density) {
            (measuredCharacterWidth * maxLineLength).toDp()
        } + 8.dp
        val editorContentWidth = maxOf(codeViewportWidth, measuredTextWidth)
        val contentHeight = maxOf(lineHeightDp * lineCount, viewportHeight)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(zoomGestureModifier)
            ) {

                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LineNumberGutter(
                        lineCount = lineCount,
                        lineHeight = lineHeightDp,
                        gutterWidth = gutterWidth,
                        scrollOffsetPx = verticalScrollState.value,
                        textMeasurer = textMeasurer,
                        textStyle = lineNumberTextStyle,
                        modifier = Modifier
                            .padding(
                                top = EDITOR_TOP_PADDING,
                                bottom = EDITOR_BOTTOM_PADDING
                            )
                    )

                    Box(
                        modifier = Modifier
                            .width(codeViewportWidth)
                            .fillMaxHeight()
                            .clipToBounds()
                            .horizontalScroll(horizontalScrollState)
                            .verticalScroll(verticalScrollState)
                            .padding(
                                top = EDITOR_TOP_PADDING,
                                bottom = EDITOR_BOTTOM_PADDING
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredWidth(editorContentWidth)
                                .height(contentHeight)
                        ) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = ::commitValue,
                                modifier = Modifier.fillMaxSize(),
                                textStyle = codeTextStyle.copy(color = Color.Transparent),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = false,
                                maxLines = Int.MAX_VALUE,
                                decorationBox = { innerTextField ->
                                    Box(Modifier.fillMaxSize()) {
                                        androidx.compose.material3.Text(
                                            text = highlightedText,
                                            style = codeTextStyle,
                                            softWrap = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            EditorActionToolbar(
                onCodeActions = { /* Reserved for future code actions. */ },
                onExpandSelection = ::selectCurrentLine,
                onSelectAll = ::selectAllText,
                onLongSelect = ::selectCurrentLine,
                onCut = ::cutSelection,
                onCopy = ::copySelection,
                onPaste = ::pasteClipboard,
                onFormatCode = { /* Formatting will be added when a formatter is available. */ },
                onHelp = { /* Help entry is intentionally reserved. */ }
            )
        }
    }
}
