package com.codeoneditor.core.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EditorActionToolbar(
    onCodeActions: () -> Unit,
    onExpandSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onLongSelect: () -> Unit,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onFormatCode: () -> Unit,
    onHelp: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = Color.Black,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorToolbarButton(Icons.Default.Code, "Ações de código", onCodeActions)
            EditorToolbarButton(Icons.Default.Fullscreen, "Expandir seleção", onExpandSelection)
            EditorToolbarButton(Icons.Default.SelectAll, "Selecionar tudo", onSelectAll)
            EditorToolbarButton(Icons.Default.TextFormat, "Seleção longa", onLongSelect)
            EditorToolbarButton(Icons.Default.ContentCut, "Recortar", onCut)
            EditorToolbarButton(Icons.Default.ContentCopy, "Copiar", onCopy)
            EditorToolbarButton(Icons.Default.ContentPaste, "Colar", onPaste)
            EditorToolbarButton(Icons.Default.FormatAlignLeft, "Formatar código", onFormatCode)
            EditorToolbarButton(Icons.Default.HelpOutline, "Ajuda", onHelp)
        }
    }
}

@Composable
private fun EditorToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}
