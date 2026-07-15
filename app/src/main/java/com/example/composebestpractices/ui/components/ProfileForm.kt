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
import com.example.composebestpractices.state.FormUiState

/**
 * Form fields:
 * - Business values (username, email, …) live in ViewModel → survive process death
 *   via SavedStateHandle if you add it; here they are in StateFlow (survive rotation).
 * - rememberSaveable is for *UI-only* ephemeral state that should survive rotation
 *   but is NOT owned by the ViewModel (e.g. local expand flag, draft hint).
 *
 * Do NOT use rememberSaveable for everything — that duplicates SSOT and can desync.
 */
@Composable
fun ProfileForm(
    state: FormUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // UI-only: survives rotation, NOT business state → rememberSaveable OK
    var showAdvancedHint by rememberSaveable { mutableStateOf(false) }

    // UI-only: OK to lose on rotation → remember is enough
    var localFocusHint by remember { mutableStateOf("Tap a field to edit") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("Profile form (VM state)")
        Spacer(modifier = Modifier.height(8.dp))

        // Each field is its own composable so changing username
        // does not needlessly dirty unrelated field layout logic.
        FormTextField(
            value = state.username,
            onValueChange = {
                localFocusHint = "Editing username"
                onUsernameChange(it)
            },
            label = "Username",
            keyboardType = KeyboardType.Text
        )
        FormTextField(
            value = state.email,
            onValueChange = {
                localFocusHint = "Editing email"
                onEmailChange(it)
            },
            label = "Email",
            keyboardType = KeyboardType.Email
        )
        FormTextField(
            value = state.company,
            onValueChange = onCompanyChange,
            label = "Company",
            keyboardType = KeyboardType.Text
        )
        FormTextField(
            value = state.notes,
            onValueChange = onNotesChange,
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
