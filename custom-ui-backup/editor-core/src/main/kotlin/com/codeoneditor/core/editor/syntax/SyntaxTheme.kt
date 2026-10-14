package com.codeoneditor.core.editor.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Syntax-only colors. The application UI/file tree remains black/white/gray;
 * these colors are used only to make source-code tokens visually distinct.
 */
object SyntaxTheme {
    val plain = SpanStyle(color = Color(0xFFE6E6E6))

    // Language keywords: if, class, fun, return, import, etc.
    val keyword = SpanStyle(color = Color(0xFFC792EA), fontWeight = FontWeight.Bold)

    // Types/classes: String, Int, ViewModel, MyClass, etc.
    val type = SpanStyle(color = Color(0xFF82AAFF), fontWeight = FontWeight.Medium)

    // Calls/function names.
    val function = SpanStyle(color = Color(0xFFFFCB6B))

    // String literals.
    val string = SpanStyle(color = Color(0xFFC3E88D))

    // Numeric literals.
    val number = SpanStyle(color = Color(0xFFF78C6C))

    // Comments stay subdued so they do not overpower code.
    val comment = SpanStyle(color = Color(0xFF7F848E), fontStyle = FontStyle.Italic)

    // Annotations such as @Composable and @Override.
    val annotation = SpanStyle(color = Color(0xFFFF5370), fontWeight = FontWeight.SemiBold)

    // true, false, null, None, NaN, etc.
    val constant = SpanStyle(color = Color(0xFFFF9CAC))

    // Operators such as =, +, ->, &&, ||.
    val operator = SpanStyle(color = Color(0xFF89DDFF))

    val property = SpanStyle(color = Color(0xFFF07178))

    // HTML/XML tags.
    val tag = SpanStyle(color = Color(0xFFFF5370), fontWeight = FontWeight.SemiBold)

    val attribute = SpanStyle(color = Color(0xFFFFCB6B))

    val punctuation = SpanStyle(color = Color(0xFF89DDFF))

    // Preprocessor/directive-like tokens.
    val directive = SpanStyle(color = Color(0xFFC792EA), fontWeight = FontWeight.SemiBold)
}
