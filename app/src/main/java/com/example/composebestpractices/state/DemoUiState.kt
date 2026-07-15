package com.example.composebestpractices.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * BEST PRACTICE — Models for Compose
 *
 * 1. Prefer `val` / data classes. Mutable `var` properties make types *unstable*
 *    for the Compose compiler, so skippability is harder and recompositions grow.
 * 2. Annotate with `@Immutable` when every property is deeply immutable.
 * 3. Do NOT bind 1:1 every composable to one giant state object. Split by
 *    **change frequency / ownership** (header vs form vs list vs footer).
 * 4. When a section would need 10–20+ params, group them into a small state
 *    object (e.g. [FormUiState]) and pass that object + lambdas.
 */

@Immutable
data class HeaderUiState(
    val title: String,
    val subtitle: String,
    val lastRefreshedLabel: String
)

/**
 * Form fields are grouped because they usually change together (typing)
 * and Header/Footer should not recompose when only username changes.
 *
 * If you ever need 20+ fields, keep grouping by domain:
 * ProfileFormState, AddressFormState, PreferencesFormState — not one mega object
 * with 40 fields that dirty the whole MiddleView.
 */
@Immutable
data class FormUiState(
    val username: String = "",
    val email: String = "",
    val company: String = "",
    val notes: String = ""
)

@Immutable
data class ListItemUiModel(
    val id: String,
    val title: String,
    val detail: String,
    val amount: String
)

@Immutable
data class TableRowUiModel(
    val id: String,
    val label: String,
    val value: String
)

@Immutable
data class FooterUiState(
    val statusMessage: String,
    val isLoading: Boolean,
    val canSubmit: Boolean
)

/**
 * Root screen state = composition of section states.
 * The screen collects this once; child composables receive ONLY their slice.
 */
@Immutable
data class DemoUiState(
    val header: HeaderUiState = HeaderUiState(
        title = "Compose Best Practices",
        subtitle = "Recomposition · State · Coroutines",
        lastRefreshedLabel = "Not loaded yet"
    ),
    val form: FormUiState = FormUiState(),
    val items: List<ListItemUiModel> = emptyList(),
    val tableRows: List<TableRowUiModel> = emptyList(),
    val footer: FooterUiState = FooterUiState(
        statusMessage = "Idle",
        isLoading = false,
        canSubmit = false
    ),
    val loadError: String? = null
)

/**
 * Event callbacks stay as stable function references from the ViewModel.
 * Prefer method references (`viewModel::onUsernameChange`) over recreating
 * lambdas in every recomposition when possible.
 */
@Stable
data class DemoUiActions(
    val onUsernameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onCompanyChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onSubmit: () -> Unit,
    val onItemClick: (String) -> Unit
)
