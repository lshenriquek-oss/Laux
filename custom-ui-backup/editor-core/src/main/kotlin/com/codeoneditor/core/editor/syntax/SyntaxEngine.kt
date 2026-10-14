package com.codeoneditor.core.editor.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.codeoneditor.core.editor.model.FileType

object SyntaxEngine {
    private val commonConstants = setOf(
        "true", "false", "null", "nil", "None",
        "TRUE", "FALSE", "NULL", "NaN"
    )

    fun highlight(text: String, fileType: FileType): AnnotatedString = buildAnnotatedString {
        append(text)
        addStyle(SyntaxTheme.plain, 0, text.length)
        if (text.isEmpty()) return@buildAnnotatedString
        val language = SyntaxRegistry.language(fileType)
        tokenize(text, language).forEach { token ->
            if (token.start >= 0 && token.start < token.end && token.end <= text.length) {
                addStyle(styleFor(token.type), token.start, token.end)
            }
        }
    }

    private fun styleFor(type: SyntaxTokenType): SpanStyle = when (type) {
        SyntaxTokenType.KEYWORD -> SyntaxTheme.keyword
        SyntaxTokenType.TYPE -> SyntaxTheme.type
        SyntaxTokenType.FUNCTION -> SyntaxTheme.function
        SyntaxTokenType.STRING -> SyntaxTheme.string
        SyntaxTokenType.NUMBER -> SyntaxTheme.number
        SyntaxTokenType.COMMENT -> SyntaxTheme.comment
        SyntaxTokenType.ANNOTATION -> SyntaxTheme.annotation
        SyntaxTokenType.CONSTANT -> SyntaxTheme.constant
        SyntaxTokenType.OPERATOR -> SyntaxTheme.operator
        SyntaxTokenType.PROPERTY -> SyntaxTheme.property
        SyntaxTokenType.TAG -> SyntaxTheme.tag
        SyntaxTokenType.ATTRIBUTE -> SyntaxTheme.attribute
        SyntaxTokenType.PUNCTUATION -> SyntaxTheme.punctuation
        SyntaxTokenType.DIRECTIVE -> SyntaxTheme.directive
        SyntaxTokenType.VARIABLE, SyntaxTokenType.PLAIN -> SyntaxTheme.plain
    }

    private fun tokenize(text: String, language: SyntaxLanguage): List<SyntaxToken> {
        val out = ArrayList<SyntaxToken>()
        val keywordSet = if (language.caseInsensitiveKeywords) {
            language.keywords.map(String::lowercase).toSet()
        } else {
            language.keywords.toSet()
        }
        val constantSet = language.constants.toSet()
        var i = 0
        while (i < text.length) {
            val block = language.blockComments.firstOrNull { text.startsWith(it.first, i) }
            if (block != null) {
                val end = text.indexOf(block.second, i + block.first.length)
                val stop = if (end < 0) text.length else end + block.second.length
                out += SyntaxToken(i, stop, SyntaxTokenType.COMMENT)
                i = stop
                continue
            }
            val lineComment = language.lineComments.firstOrNull { text.startsWith(it, i) }
            if (lineComment != null) {
                val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                out += SyntaxToken(i, end, SyntaxTokenType.COMMENT)
                i = end
                continue
            }
            val c = text[i]
            if (language.hashComments && c == '#') {
                val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                out += SyntaxToken(i, end, SyntaxTokenType.COMMENT)
                i = end
                continue
            }
            if (language.markup && c == '<') {
                val end = readMarkupTag(text, i)
                if (end > i) { out += SyntaxToken(i, end, SyntaxTokenType.TAG); i = end; continue }
            }
            if (c == '"' || c == '\'' || c == '`') {
                val end = readString(text, i, c, language.fileType)
                out += SyntaxToken(i, end, SyntaxTokenType.STRING)
                i = end
                continue
            }
            if (c.isDigit() && (i == 0 || !text[i - 1].isLetterOrDigit() && text[i - 1] != '_')) {
                val end = readNumber(text, i)
                out += SyntaxToken(i, end, SyntaxTokenType.NUMBER)
                i = end
                continue
            }
            if (c == '@' && i + 1 < text.length && text[i + 1].isLetter()) {
                var end = i + 2
                while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_' || text[end] == '.')) end++
                out += SyntaxToken(i, end, SyntaxTokenType.ANNOTATION)
                i = end
                continue
            }
            if (c.isLetter() || c == '_' || c == '$') {
                var end = i + 1
                while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_' || text[end] == '$')) end++
                val word = text.substring(i, end)
                val normalized = if (language.caseInsensitiveKeywords) word.lowercase() else word
                val next = skipWhitespace(text, end)
                val tokenType = when {
                    keywordSet.contains(normalized) -> SyntaxTokenType.KEYWORD
                    word in constantSet || word in commonConstants -> SyntaxTokenType.CONSTANT
                    word in language.types || (word.firstOrNull()?.isUpperCase() == true && word.length > 1) -> SyntaxTokenType.TYPE
                    next < text.length && text[next] == '(' -> SyntaxTokenType.FUNCTION
                    else -> SyntaxTokenType.PLAIN
                }
                out += SyntaxToken(i, end, tokenType)
                i = end
                continue
            }
            if (c in "{}[]();,.:" && !language.markup) out += SyntaxToken(i, i + 1, SyntaxTokenType.PUNCTUATION)
            else if (c in "=+-*/%!<>?&|^~") out += SyntaxToken(i, i + 1, SyntaxTokenType.OPERATOR)
            i++
        }
        return out
    }

    private fun readString(text: String, start: Int, quote: Char, type: FileType): Int {
        if ((type == FileType.KOTLIN || type == FileType.PYTHON) && start + 2 < text.length && text[start] == quote && text[start + 1] == quote && text[start + 2] == quote) {
            val marker = """$quote$quote$quote"""
            val end = text.indexOf(marker, start + 3)
            return if (end < 0) text.length else end + 3
        }
        var i = start + 1
        var escaped = false
        while (i < text.length) {
            val c = text[i]
            if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == quote) return i + 1
            i++
        }
        return text.length
    }

    private fun readNumber(text: String, start: Int): Int {
        var i = start
        while (i < text.length && (text[i].isLetterOrDigit() || text[i] in "._+-")) {
            if ((text[i] == '+' || text[i] == '-') && i > start && text[i - 1] !in "eE") break
            i++
        }
        return i
    }

    private fun readMarkupTag(text: String, start: Int): Int {
        var i = start + 1
        var quote: Char? = null
        while (i < text.length) {
            val c = text[i]
            if (quote != null) { if (c == quote && text.getOrNull(i - 1) != '\\') quote = null }
            else if (c == '"' || c == '\'') quote = c
            else if (c == '>') return i + 1
            i++
        }
        return if (i > start + 1) i else start
    }

    private fun skipWhitespace(text: String, start: Int): Int { var i = start; while (i < text.length && text[i].isWhitespace()) i++; return i }
}
