package com.codeoneditor.core.editor

import com.codeoneditor.core.editor.model.EditorDocument

/** Reusable document engine. It contains editor state and text operations, not application UI. */
class EditorEngine(initial: EditorDocument) {
    private var documentState = initial
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    val document: EditorDocument get() = documentState
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun replaceText(newText: String, cursor: Int = newText.length) {
        if (newText == documentState.content) return
        undoStack.add(documentState.content)
        redoStack.clear()
        val safeCursor = cursor.coerceIn(0, newText.length)
        documentState = documentState.copy(
            content = newText,
            cursor = safeCursor,
            selectionStart = safeCursor,
            selectionEnd = safeCursor
        )
    }

    fun replaceExternalContent(newText: String, cursor: Int = newText.length) {
        val safeCursor = cursor.coerceIn(0, newText.length)
        documentState = documentState.copy(
            content = newText,
            cursor = safeCursor,
            selectionStart = safeCursor,
            selectionEnd = safeCursor
        )
        undoStack.clear()
        redoStack.clear()
    }

    fun setSelection(start: Int, end: Int = start) {
        val safeStart = start.coerceIn(0, documentState.content.length)
        val safeEnd = end.coerceIn(0, documentState.content.length)
        documentState = documentState.copy(
            cursor = safeEnd,
            selectionStart = safeStart,
            selectionEnd = safeEnd
        )
    }

    fun undo(): EditorDocument? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.add(documentState.content)
        documentState = documentState.copy(content = previous, cursor = previous.length, selectionStart = previous.length, selectionEnd = previous.length)
        return documentState
    }

    fun redo(): EditorDocument? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.add(documentState.content)
        documentState = documentState.copy(content = next, cursor = next.length, selectionStart = next.length, selectionEnd = next.length)
        return documentState
    }
}
