package com.codeoneditor.core.editor.syntax

import com.codeoneditor.core.editor.model.FileType
import com.codeoneditor.core.editor.syntax.languages.definitionFor

object SyntaxRegistry {
    private val cache = mutableMapOf<FileType, SyntaxLanguage>()
    fun language(type: FileType): SyntaxLanguage = cache.getOrPut(type) { definitionFor(type) }
}
