package com.example.composebestpractices.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
 * over many individual params.
 *
 * Children still receive *slices* so table/list can skip when only the form changes.
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
                text = "Services (unique keys)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(
            items = state.items,
            key = { it.id }
        ) { item ->
            ServiceRow(
                item = item,
                onClick = { actions.onItemClick(item.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ServiceRow(
    item: ListItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StableLabel(text = item.title, style = LabelStyle.Body)
                StableLabel(text = item.detail, style = LabelStyle.Caption)
            }
            StableLabel(text = item.amount, style = LabelStyle.Body)
        }
    }
}
