package com.codeoneditor.core.editor.syntax

import com.codeoneditor.core.editor.model.FileType
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxEngineTest {
    @Test fun preservesSourceText() {
        val source = "val text = \"fun class return\""
        assertTrue(SyntaxEngine.highlight(source, FileType.KOTLIN).text == source)
    }
    @Test fun highlightsKotlinSource() {
        val result = SyntaxEngine.highlight("fun main() { val x = 42 }", FileType.KOTLIN)
        assertTrue(result.text.contains("fun main"))
    }
}
