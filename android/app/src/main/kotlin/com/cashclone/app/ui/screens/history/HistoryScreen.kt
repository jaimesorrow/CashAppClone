package com.cashclone.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cashclone.app.data.model.TransferResponse
import com.cashclone.app.viewmodel.HistoryViewModel
import com.cashclone.app.viewmodel.UiState

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Activity", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        when (val current = state) {
            is UiState.Loading, UiState.Idle -> CircularProgressIndicator()
            is UiState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is UiState.Success -> LazyColumn {
                items(current.data) { transfer ->
                    HistoryRow(transfer, onFulfill = { viewModel.fulfill(transfer.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(transfer: TransferResponse, onFulfill: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val verb = if (transfer.direction == "OUTGOING") "To" else "From"
            Text("$verb \$${transfer.counterpartyCashtag}")
            val sign = if (transfer.direction == "OUTGOING") "-" else "+"
            Text("$sign$%.2f".format(transfer.amountCents / 100.0))
        }
        transfer.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("${transfer.type} · ${transfer.status}", style = MaterialTheme.typography.bodySmall)

        val canFulfill = transfer.type == "REQUEST" && transfer.status == "PENDING" && transfer.direction == "OUTGOING"
        if (canFulfill) {
            Button(onClick = onFulfill) { Text("Pay") }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
