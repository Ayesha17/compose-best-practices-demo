package com.example.composebestpractices.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composebestpractices.state.DemoUiState
import com.example.composebestpractices.state.FooterUiState
import com.example.composebestpractices.state.FormUiState
import com.example.composebestpractices.state.ListItemUiModel
import com.example.composebestpractices.state.TableRowUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BEST PRACTICE — ViewModel state & coroutines
 *
 * ## Who owns state?
 * ViewModel owns [uiState]. Composables never mutate shared business state with
 * `var` in models — they send events (`onUsernameChange`) and observe flows.
 *
 * ## Can another ViewModel function access updated state?
 * YES. After `setUsername`, any other VM method should read
 * `_uiState.value` (or the flow). Do NOT thread state through UI parameters
 * back into the ViewModel for readability — the UI is not the source of truth.
 *
 * Good:  `val current = _uiState.value.form.username`
 * Bad:   UI passes username into `submit(username)` when VM already has it
 *        (unless you intentionally pass an ephemeral draft).
 *
 * ## When 2–3 functions update the same object:
 * Always update via `_uiState.update { }` so updates are atomic and Compose
 * sees one new immutable snapshot.
 *
 * ## Dispatchers
 * - [Dispatchers.IO]: network / disk / database
 * - [Dispatchers.Default]: CPU (sort, map, parse large JSON)
 * - [Dispatchers.Main]: UI / StateFlow publish (viewModelScope default is Main)
 *
 * Inject dispatchers (as below) so unit tests can replace them with a TestDispatcher.
 */
class DemoViewModel(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    // region Form updates — each touches only the form slice

    fun onUsernameChange(value: String) {
        _uiState.update { state ->
            val form = state.form.copy(username = value)
            state.copy(
                form = form,
                footer = state.footer.copy(canSubmit = form.isReadyToSubmit())
            )
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { state ->
            val form = state.form.copy(email = value)
            state.copy(
                form = form,
                footer = state.footer.copy(canSubmit = form.isReadyToSubmit())
            )
        }
    }

    fun onCompanyChange(value: String) {
        _uiState.update { state ->
            val form = state.form.copy(company = value)
            state.copy(
                form = form,
                footer = state.footer.copy(canSubmit = form.isReadyToSubmit())
            )
        }
    }

    fun onNotesChange(value: String) {
        _uiState.update { state ->
            state.copy(form = state.form.copy(notes = value))
        }
    }

    // endregion

    /**
     * `viewModelScope.launch { }` means:
     * - Start a coroutine tied to this ViewModel's lifecycle.
     * - When the ViewModel is cleared (user leaves the screen permanently),
     *   the scope is cancelled — no leaking work / crashes after destroy.
     * - Default dispatcher for viewModelScope is Main (UI thread), so switch
     *   explicitly for heavy / IO work.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    footer = it.footer.copy(isLoading = true, statusMessage = "Loading…"),
                    loadError = null
                )
            }

            try {
                // Parallel / concurrent network-like calls with async {}
                val itemsDeferred = async(ioDispatcher) { fetchItemsFromNetwork() }
                val statsDeferred = async(ioDispatcher) { fetchStatsFromNetwork() }
                // CPU-ish work on Default (sorting / mapping large lists)
                val mappedDeferred = async(defaultDispatcher) {
                    delay(80) // simulate mapping work
                    "Mapped on Default @ ${System.currentTimeMillis() % 100_000}"
                }

                val items = itemsDeferred.await()
                val stats = statsDeferred.await()
                val mappedLabel = mappedDeferred.await()

                // Back on Main (viewModelScope default) — safe to publish UI state
                _uiState.update { state ->
                    state.copy(
                        header = state.header.copy(
                            lastRefreshedLabel = "Refreshed · $mappedLabel"
                        ),
                        items = items,
                        tableRows = stats,
                        footer = state.footer.copy(
                            isLoading = false,
                            statusMessage = "Loaded ${items.size} items",
                            canSubmit = state.form.isReadyToSubmit()
                        ),
                        loadError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        footer = it.footer.copy(
                            isLoading = false,
                            statusMessage = "Failed"
                        ),
                        loadError = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    /**
     * Reads the *same* state object updated by onUsernameChange / onEmailChange
     * directly from `_uiState.value` — best practice for readability and SSOT.
     */
    fun submit() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            if (!snapshot.form.isReadyToSubmit() || snapshot.footer.isLoading) return@launch

            _uiState.update {
                it.copy(footer = it.footer.copy(isLoading = true, statusMessage = "Submitting…"))
            }

            // Example: do IO off Main, then hop back implicitly via viewModelScope
            val result = withContext(ioDispatcher) {
                delay(400)
                "Saved user=${snapshot.form.username}, email=${snapshot.form.email}"
            }

            // Another VM function can also read the latest state here:
            val latestUsername = _uiState.value.form.username

            _uiState.update {
                it.copy(
                    footer = FooterUiState(
                        statusMessage = "$result (latest user=$latestUsername)",
                        isLoading = false,
                        canSubmit = it.form.isReadyToSubmit()
                    )
                )
            }
        }
    }

    fun onItemClick(id: String) {
        // Reading state owned by VM — not from UI params
        val title = _uiState.value.items.firstOrNull { it.id == id }?.title ?: id
        _uiState.update {
            it.copy(footer = it.footer.copy(statusMessage = "Clicked: $title"))
        }
    }

    private suspend fun fetchItemsFromNetwork(): List<ListItemUiModel> =
        withContext(ioDispatcher) {
            delay(350)
            List(12) { index ->
                ListItemUiModel(
                    id = "item-$index",
                    title = "Service #${index + 1}",
                    detail = "Plan tier ${(index % 3) + 1}",
                    amount = "${(index + 1) * 15} SAR"
                )
            }
        }

    private suspend fun fetchStatsFromNetwork(): List<TableRowUiModel> =
        withContext(ioDispatcher) {
            delay(280)
            listOf(
                TableRowUiModel("r1", "Active lines", "128"),
                TableRowUiModel("r2", "Open tickets", "7"),
                TableRowUiModel("r3", "Monthly spend", "42,500 SAR"),
                TableRowUiModel("r4", "Agents online", "14")
            )
        }

    private fun FormUiState.isReadyToSubmit(): Boolean =
        username.isNotBlank() && email.contains("@") && company.isNotBlank()
}
