package com.example.composebestpractices.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.composebestpractices.state.TableRowUiModel

/**
 * Table as ViewComponents:
 * - Outer frame receives only [rows].
 * - Each row is a separate @Composable keyed by id → only dirty rows recompose.
 * - For huge tables, switch the body to LazyColumn { items(rows, key = { it.id }) }.
 */
@Composable
fun StatsTable(
    rows: List<TableRowUiModel>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Stats table (row components)",
            style = MaterialTheme.typography.titleMedium
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            TableHeaderRow()
            rows.forEach { row ->
                key(row.id) {
                    TableDataRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text("Metric", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Text("Value", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TableDataRow(
    row: TableRowUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        StableLabel(text = row.label, style = LabelStyle.Body, modifier = Modifier.weight(1f))
        StableLabel(text = row.value, style = LabelStyle.Body, modifier = Modifier.weight(1f))
    }
}
