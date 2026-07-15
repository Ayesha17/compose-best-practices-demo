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
 * 4. When a section would need many params, group into:
 *    - section state object (e.g. [MiddleUiState])
 *    - section actions object (e.g. [MiddleUiActions])
 *    Prefer ~2–3 params over 10–20 individual ones.
 */

@Immutable
data class HeaderUiState(
    val title: String,
    val subtitle: String,
    val lastRefreshedLabel: String
)

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

/**
 * Everything MiddleView needs to *read*.
 * Typing still creates a new [MiddleUiState], but children only get their slice
 * (form / table / items) so table/list can skip when those lists are unchanged.
 */
@Immutable
data class MiddleUiState(
    val form: FormUiState = FormUiState(),
    val tableRows: List<TableRowUiModel> = emptyList(),
    val items: List<ListItemUiModel> = emptyList(),
    val loadError: String? = null
)

@Immutable
data class FooterUiState(
    val statusMessage: String,
    val isLoading: Boolean,
    val canSubmit: Boolean
)

@Immutable
data class DemoUiState(
    val header: HeaderUiState = HeaderUiState(
        title = "Compose Best Practices",
        subtitle = "Recomposition · State · Coroutines",
        lastRefreshedLabel = "Not loaded yet"
    ),
    val middle: MiddleUiState = MiddleUiState(),
    val footer: FooterUiState = FooterUiState(
        statusMessage = "Idle",
        isLoading = false,
        canSubmit = false
    )
)

/** Form callbacks grouped — avoid 4 separate lambda params. */
@Stable
data class FormUiActions(
    val onUsernameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onCompanyChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit
)

/** Middle section callbacks. */
@Stable
data class MiddleUiActions(
    val form: FormUiActions,
    val onItemClick: (String) -> Unit
)

@Stable
data class FooterUiActions(
    val onRefresh: () -> Unit,
    val onSubmit: () -> Unit
)
