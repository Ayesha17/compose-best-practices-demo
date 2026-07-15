package com.example.composebestpractices.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.composebestpractices.state.ListItemUiModel
import com.example.composebestpractices.state.MiddleUiActions
import com.example.composebestpractices.state.MiddleUiState

/**
 * Prefer a small signature:
 *   MiddleView(state, actions, modifier)
 *
 * TextField in list: each row gets value from [ListItemUiModel.note] and
 * notifies ViewModel via [MiddleUiActions.onItemNoteChange].
 */
@Composable
fun MiddleView(
    state: MiddleUiState,
    actions: MiddleUiActions,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "form") {
            ProfileForm(
                state = state.form,
                actions = actions.form
            )
        }

        if (state.loadError != null) {
            item(key = "error") {
                Text(
                    text = "Error: ${state.loadError}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item(key = "table") {
            StatsTable(rows = state.tableRows)
        }

        item(key = "list-header") {
            Text(
                text = "Services (list + TextField per row)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Text(
                text = "Type a note in a row, scroll away, come back — text stays (VM owns it).",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        items(
            items = state.items,
            key = { it.id }
        ) { item ->
            // Pass stable actions + item. Do NOT wrap:
            //   onNoteChange = { note -> actions.onItemNoteChange(item.id, note) }
            // that creates a new lambda per row on every parent recomposition
            // and forces every visible row to recompose.
            ServiceRow(
                item = item,
                onTitleClick = actions.onItemClick,
                onNoteChange = actions.onItemNoteChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * List row ViewComponent.
 *
 * When one note changes:
 * - Parent builds a *new* list (normal / correct).
 * - Unchanged items keep the *same object* (`else item` in map) → those rows can skip.
 * - Changed item is a new copy → only that row needs to run again.
 * Use stable keys + stable callbacks (method refs), not new lambdas each time.
 */
@Composable
private fun ServiceRow(
    item: ListItemUiModel,
    onTitleClick: (String) -> Unit,
    onNoteChange: (id: String, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onTitleClick(item.id) }),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StableLabel(text = item.title, style = LabelStyle.Body)
                    StableLabel(text = item.detail, style = LabelStyle.Caption)
                }
                StableLabel(text = item.amount, style = LabelStyle.Body)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = item.note,
                onValueChange = { note -> onNoteChange(item.id, note) },
                label = { Text("Note for ${item.title}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
