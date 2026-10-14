package com.codeoneditor.core.editor.syntax

import androidx.compose.ui.text.AnnotatedString
import com.codeoneditor.core.editor.model.FileType

object CodeSyntaxHighlighter {
    fun highlightCode(text: String, fileType: FileType): AnnotatedString = SyntaxEngine.highlight(text, fileType)
}
