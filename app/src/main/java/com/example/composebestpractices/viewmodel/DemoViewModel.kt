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
 * Write via `_uiState.update { }`, read via `_uiState.value` (single source of truth).
 *
 * Dispatchers:
 * - IO: network / disk / DB
 * - Default: CPU work
 * - Main: default for viewModelScope (UI / publishing state)
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

    fun onUsernameChange(value: String) {
        updateForm { it.copy(username = value) }
    }

    fun onEmailChange(value: String) {
        updateForm { it.copy(email = value) }
    }

    fun onCompanyChange(value: String) {
        updateForm { it.copy(company = value) }
    }

    fun onNotesChange(value: String) {
        updateForm { it.copy(notes = value) }
    }

    private fun updateForm(transform: (FormUiState) -> FormUiState) {
        _uiState.update { state ->
            val form = transform(state.middle.form)
            state.copy(
                middle = state.middle.copy(form = form),
                footer = state.footer.copy(canSubmit = form.isReadyToSubmit())
            )
        }
    }

    /**
     * `viewModelScope.launch` = coroutine cancelled when ViewModel is cleared.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    footer = it.footer.copy(isLoading = true, statusMessage = "Loading…"),
                    middle = it.middle.copy(loadError = null)
                )
            }

            try {
                val itemsDeferred = async(ioDispatcher) { fetchItemsFromNetwork() }
                val statsDeferred = async(ioDispatcher) { fetchStatsFromNetwork() }
                val mappedDeferred = async(defaultDispatcher) {
                    delay(80)
                    "Mapped on Default @ ${System.currentTimeMillis() % 100_000}"
                }

                val items = itemsDeferred.await()
                val stats = statsDeferred.await()
                val mappedLabel = mappedDeferred.await()

                _uiState.update { state ->
                    state.copy(
                        header = state.header.copy(
                            lastRefreshedLabel = "Refreshed · $mappedLabel"
                        ),
                        middle = state.middle.copy(
                            items = items,
                            tableRows = stats,
                            loadError = null
                        ),
                        footer = state.footer.copy(
                            isLoading = false,
                            statusMessage = "Loaded ${items.size} items",
                            canSubmit = state.middle.form.isReadyToSubmit()
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        footer = it.footer.copy(
                            isLoading = false,
                            statusMessage = "Failed"
                        ),
                        middle = it.middle.copy(
                            loadError = e.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }

    fun submit() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            if (!snapshot.middle.form.isReadyToSubmit() || snapshot.footer.isLoading) return@launch

            _uiState.update {
                it.copy(footer = it.footer.copy(isLoading = true, statusMessage = "Submitting…"))
            }

            val result = withContext(ioDispatcher) {
                delay(400)
                "Saved user=${snapshot.middle.form.username}, email=${snapshot.middle.form.email}"
            }

            val latestUsername = _uiState.value.middle.form.username

            _uiState.update {
                it.copy(
                    footer = FooterUiState(
                        statusMessage = "$result (latest user=$latestUsername)",
                        isLoading = false,
                        canSubmit = it.middle.form.isReadyToSubmit()
                    )
                )
            }
        }
    }

    fun onItemClick(id: String) {
        val title = _uiState.value.middle.items.firstOrNull { it.id == id }?.title ?: id
        _uiState.update {
            it.copy(footer = it.footer.copy(statusMessage = "Clicked: $title"))
        }
    }

    /**
     * TextField inside LazyColumn: keep the typed text in the list item model
     * (ViewModel), not in remember inside the row. Scrolling off-screen would
     * otherwise lose local remember state when the item leaves composition.
     */
    fun onItemNoteChange(id: String, note: String) {
        _uiState.update { state ->
            val updatedItems = state.middle.items.map { item ->
                if (item.id == id) item.copy(note = note) else item
            }
            state.copy(middle = state.middle.copy(items = updatedItems))
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
                    amount = "${(index + 1) * 15} SAR",
                    note = ""
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
