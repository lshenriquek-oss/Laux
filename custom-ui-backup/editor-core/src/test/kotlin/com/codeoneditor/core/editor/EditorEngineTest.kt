package com.codeoneditor.core.editor

import com.codeoneditor.core.editor.model.EditorDocument
import com.codeoneditor.core.editor.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorEngineTest {
    private fun engine() = EditorEngine(
        EditorDocument(
            fileId = "1",
            fileName = "main.kt",
            filePath = "project/main.kt",
            fileType = FileType.KOTLIN,
            content = "one"
        )
    )

    @Test
    fun replaceAndUndoRedoKeepDocumentState() {
        val engine = engine()
        engine.replaceText("two")
        assertEquals("two", engine.document.content)
        assertTrue(engine.canUndo)
        engine.undo()
        assertEquals("one", engine.document.content)
        assertTrue(engine.canRedo)
        engine.redo()
        assertEquals("two", engine.document.content)
    }

    @Test
    fun selectionIsClampedToDocumentBounds() {
        val engine = engine()
        engine.setSelection(-10, 100)
        assertEquals(0, engine.document.selectionStart)
        assertEquals(3, engine.document.selectionEnd)
    }
}
