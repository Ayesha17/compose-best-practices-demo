package com.example.composebestpractices.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.composebestpractices.state.FormUiActions
import com.example.composebestpractices.state.FormUiState

/**
 * Form fields:
 * - Business values live in ViewModel StateFlow.
 * - rememberSaveable is for UI-only flags, not business data.
 * - Signature stays small: state + actions (not 4 separate lambdas).
 */
@Composable
fun ProfileForm(
    state: FormUiState,
    actions: FormUiActions,
    modifier: Modifier = Modifier
) {
    var showAdvancedHint by rememberSaveable { mutableStateOf(false) }
    var localFocusHint by remember { mutableStateOf("Tap a field to edit") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("Profile form (VM state)")
        Spacer(modifier = Modifier.height(8.dp))

        FormTextField(
            value = state.username,
            onValueChange = {
                localFocusHint = "Editing username"
                actions.onUsernameChange(it)
            },
            label = "Username",
            keyboardType = KeyboardType.Text
        )
        FormTextField(
            value = state.email,
            onValueChange = {
                localFocusHint = "Editing email"
                actions.onEmailChange(it)
            },
            label = "Email",
            keyboardType = KeyboardType.Email
        )
        FormTextField(
            value = state.company,
            onValueChange = actions.onCompanyChange,
            label = "Company",
            keyboardType = KeyboardType.Text
        )
        FormTextField(
            value = state.notes,
            onValueChange = actions.onNotesChange,
            label = "Notes",
            keyboardType = KeyboardType.Text,
            singleLine = false
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = localFocusHint)
        androidx.compose.material3.TextButton(onClick = { showAdvancedHint = !showAdvancedHint }) {
            Text(if (showAdvancedHint) "Hide tip" else "Show tip")
        }
        if (showAdvancedHint) {
            Text("Tip: hoist text to ViewModel; use rememberSaveable only for UI flags.")
        }
    }
}

/**
 * Isolated TextField leaf — only recomposes when [value]/[label] change.
 * Prefer OutlinedTextField(value, onValueChange) with hoisted state.
 * Other items that need similar care: Slider, Switch, Checkbox, FilterChip —
 * hoist checked/value to parent/VM; pass primitives + lambdas.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = keyboardType
        )
    )
}
