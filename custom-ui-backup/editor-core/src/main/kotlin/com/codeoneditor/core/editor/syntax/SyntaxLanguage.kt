package com.codeoneditor.core.editor.syntax

import com.codeoneditor.core.editor.model.FileType

enum class SyntaxTokenType { KEYWORD, TYPE, FUNCTION, STRING, NUMBER, COMMENT, ANNOTATION, CONSTANT, OPERATOR, PROPERTY, TAG, ATTRIBUTE, PUNCTUATION, DIRECTIVE, VARIABLE, PLAIN }
data class SyntaxToken(val start: Int, val end: Int, val type: SyntaxTokenType)
data class SyntaxLanguage(
    val fileType: FileType,
    val lineComments: List<String> = emptyList(),
    val blockComments: List<Pair<String, String>> = emptyList(),
    val keywords: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val constants: Set<String> = emptySet(),
    val caseInsensitiveKeywords: Boolean = false,
    val hashComments: Boolean = false,
    val markup: Boolean = false,
    val cssLike: Boolean = false,
    val jsonLike: Boolean = false,
    val markdown: Boolean = false
)
