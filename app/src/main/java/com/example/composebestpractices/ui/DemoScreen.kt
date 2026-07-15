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
import com.example.composebestpractices.state.FooterUiActions
import com.example.composebestpractices.state.FormUiActions
import com.example.composebestpractices.state.MiddleUiActions
import com.example.composebestpractices.ui.components.DemoFooter
import com.example.composebestpractices.ui.components.DemoHeader
import com.example.composebestpractices.ui.components.MiddleView
import com.example.composebestpractices.viewmodel.DemoViewModel

/**
 * Screen orchestration: collect once, pass section state + section actions.
 *
 *   DemoHeader(state)
 *   MiddleView(state, actions)   ← not 9+ loose params
 *   DemoFooter(state, actions)
 */
@Composable
fun DemoScreen(
    viewModel: DemoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val middleActions = remember(viewModel) {
        MiddleUiActions(
            form = FormUiActions(
                onUsernameChange = viewModel::onUsernameChange,
                onEmailChange = viewModel::onEmailChange,
                onCompanyChange = viewModel::onCompanyChange,
                onNotesChange = viewModel::onNotesChange
            ),
            onItemClick = viewModel::onItemClick,
            onItemNoteChange = viewModel::onItemNoteChange
        )
    }
    val footerActions = remember(viewModel) {
        FooterUiActions(
            onRefresh = viewModel::refresh,
            onSubmit = viewModel::submit
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoHeader(
            state = uiState.header,
            modifier = Modifier.fillMaxWidth()
        )

        MiddleView(
            state = uiState.middle,
            actions = middleActions,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        DemoFooter(
            state = uiState.footer,
            actions = footerActions,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
