package com.cashclone.app.ui.screens.kyc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cashclone.app.data.model.KycApplicationRequest
import com.cashclone.app.viewmodel.KycViewModel
import com.cashclone.app.viewmodel.UiState

@Composable
fun KycScreen(viewModel: KycViewModel, onApproved: () -> Unit) {
    var dateOfBirth by remember { mutableStateOf("") }
    var ssn by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateField by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(uiState) {
        val current = uiState
        if (current is UiState.Success && current.data.kycStatus == "APPROVED") onApproved()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Verify your identity", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Required by federal law before you can send or receive real money.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(dateOfBirth, { dateOfBirth = it }, label = { Text("Date of birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(ssn, { ssn = it }, label = { Text("SSN") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(street, { street = it }, label = { Text("Street address") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(stateField, { stateField = it }, label = { Text("State (e.g. CA)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(postalCode, { postalCode = it }, label = { Text("Postal code") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))

        when (uiState) {
            is UiState.Loading -> CircularProgressIndicator()
            else -> Button(
                onClick = {
                    viewModel.submit(
                        KycApplicationRequest(
                            dateOfBirth = dateOfBirth.trim(),
                            ssn = ssn.trim(),
                            addressStreet = street.trim(),
                            addressCity = city.trim(),
                            addressState = stateField.trim(),
                            addressPostalCode = postalCode.trim(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = listOf(dateOfBirth, ssn, street, city, stateField, postalCode).all { it.isNotBlank() },
            ) { Text("Submit") }
        }

        val current = uiState
        if (current is UiState.Success) {
            Spacer(Modifier.height(12.dp))
            Text("Status: ${current.data.kycStatus}")
        }
        if (current is UiState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(current.message, color = MaterialTheme.colorScheme.error)
        }
    }
}
