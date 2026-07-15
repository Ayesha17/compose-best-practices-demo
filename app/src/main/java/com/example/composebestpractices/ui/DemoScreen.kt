package com.example.composebestpractices.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.composebestpractices.state.DemoUiActions
import com.example.composebestpractices.ui.components.DemoFooter
import com.example.composebestpractices.ui.components.DemoHeader
import com.example.composebestpractices.ui.components.MiddleView
import com.example.composebestpractices.viewmodel.DemoViewModel

/**
 * Screen orchestration ONLY.
 *
 * Pattern:
 * 1. Collect root state once.
 * 2. Pass **slices** (header / form / footer) — not the whole state — to children.
 * 3. Hold action lambdas in a remembered [DemoUiActions] so child stability improves.
 *
 * Header / Middle / Footer are separate functions so typing in the form
 * does not force Header to recompose when [HeaderUiState] equals the previous value.
 */
@Composable
fun DemoScreen(
    viewModel: DemoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val actions = remember(viewModel) {
        DemoUiActions(
            onUsernameChange = viewModel::onUsernameChange,
            onEmailChange = viewModel::onEmailChange,
            onCompanyChange = viewModel::onCompanyChange,
            onNotesChange = viewModel::onNotesChange,
            onRefresh = viewModel::refresh,
            onSubmit = viewModel::submit,
            onItemClick = viewModel::onItemClick
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoHeader(
            state = uiState.header,
            modifier = Modifier.fillMaxWidth()
        )

        MiddleView(
            form = uiState.form,
            tableRows = uiState.tableRows,
            items = uiState.items,
            loadError = uiState.loadError,
            onUsernameChange = actions.onUsernameChange,
            onEmailChange = actions.onEmailChange,
            onCompanyChange = actions.onCompanyChange,
            onNotesChange = actions.onNotesChange,
            onItemClick = actions.onItemClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        DemoFooter(
            state = uiState.footer,
            onRefresh = actions.onRefresh,
            onSubmit = actions.onSubmit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
