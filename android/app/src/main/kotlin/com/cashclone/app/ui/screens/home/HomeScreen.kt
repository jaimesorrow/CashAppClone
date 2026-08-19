package com.cashclone.app.ui.screens.home

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cashclone.app.data.model.TransferResponse
import com.cashclone.app.viewmodel.HomeViewModel
import com.cashclone.app.viewmodel.UiState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSend: () -> Unit,
    onRequest: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
    onNeedsKyc: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CashClone", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onProfile) { Text("Profile") }
        }
        Spacer(Modifier.height(24.dp))

        when (val current = state) {
            is UiState.Loading, UiState.Idle -> CircularProgressIndicator()
            is UiState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is UiState.Success -> {
                val data = current.data
                if (data.profile.kycStatus != "APPROVED") {
                    Text("Finish verifying your identity to send or receive money.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onNeedsKyc, modifier = Modifier.fillMaxWidth()) { Text("Verify identity") }
                } else {
                    val cents = data.balance?.availableCents ?: 0L
                    Text(
                        "$%.2f".format(cents / 100.0),
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Text("Available balance", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onSend, modifier = Modifier.weight(1f)) { Text("Send") }
                        OutlinedButton(onClick = onRequest, modifier = Modifier.weight(1f)) { Text("Request") }
                    }
                }

                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent activity", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = onHistory) { Text("See all") }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(data.recentActivity) { transfer -> ActivityRow(transfer) }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(transfer: TransferResponse) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val verb = if (transfer.direction == "OUTGOING") "To" else "From"
            Text("$verb \$${transfer.counterpartyCashtag}")
            val sign = if (transfer.direction == "OUTGOING") "-" else "+"
            Text("$sign$%.2f".format(transfer.amountCents / 100.0))
        }
        Text(transfer.status, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
