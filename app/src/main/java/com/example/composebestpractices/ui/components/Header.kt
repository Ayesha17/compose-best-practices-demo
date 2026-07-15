package com.example.composebestpractices.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.composebestpractices.state.HeaderUiState

/**
 * Header only depends on [HeaderUiState].
 * Typing in MiddleView (form) does NOT recompose this when equality holds.
 *
 * Pass specific section state — not the entire DemoUiState.
 */
@Composable
fun DemoHeader(
    state: HeaderUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        // Stable primitives (String) — cheap skip if unchanged
        StableLabel(
            text = state.title,
            style = LabelStyle.Title,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        StableLabel(
            text = state.subtitle,
            style = LabelStyle.Body,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        StableLabel(
            text = state.lastRefreshedLabel,
            style = LabelStyle.Caption,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

enum class LabelStyle { Title, Body, Caption }

/**
 * Extracted label/text — receives only the String it displays.
 * Avoids pulling parent state into a tiny leaf that recomposes often.
 */
@Composable
fun StableLabel(
    text: String,
    style: LabelStyle,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    val textStyle = when (style) {
        LabelStyle.Title -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        LabelStyle.Body -> MaterialTheme.typography.bodyMedium
        LabelStyle.Caption -> MaterialTheme.typography.labelMedium
    }
    Text(text = text, style = textStyle, color = color, modifier = modifier)
}
