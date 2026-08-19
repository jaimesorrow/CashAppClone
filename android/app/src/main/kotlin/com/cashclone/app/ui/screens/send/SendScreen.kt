package com.cashclone.app.ui.screens.send

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cashclone.app.viewmodel.SendRequestViewModel
import com.cashclone.app.viewmodel.UiState

@Composable
fun SendScreen(viewModel: SendRequestViewModel, onDone: () -> Unit) {
    var cashtag by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val transferState by viewModel.transferState.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    LaunchedEffect(transferState) {
        if (transferState is UiState.Success) onDone()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Send money", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = cashtag,
            onValueChange = {
                cashtag = it
                viewModel.searchCashtag(it.removePrefix("$"))
            },
            label = { Text("Recipient cashtag") },
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn {
            items(results) { user ->
                TextButton(onClick = { cashtag = user.cashtag }) {
                    Text("\$${user.cashtag} — ${user.fullName}")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Amount (USD)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(note, { note = it }, label = { Text("What's it for? (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))

        val amountCents = (amount.toDoubleOrNull() ?: 0.0).let { (it * 100).toLong() }
        if (transferState is UiState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.send(cashtag.removePrefix("$").trim(), amountCents, note.trim().ifBlank { null }) },
                modifier = Modifier.fillMaxWidth(),
                enabled = cashtag.isNotBlank() && amountCents > 0,
            ) { Text("Send \$%.2f".format(amountCents / 100.0)) }
        }
        if (transferState is UiState.Error) {
            Spacer(Modifier.height(12.dp))
            Text((transferState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
        }
    }
}
