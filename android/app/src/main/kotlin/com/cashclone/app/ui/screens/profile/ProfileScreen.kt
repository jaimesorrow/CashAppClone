package com.cashclone.app.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cashclone.app.data.model.UserProfile
import com.cashclone.app.data.repository.CashCloneRepository
import com.cashclone.app.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(repository: CashCloneRepository, authViewModel: AuthViewModel, onLoggedOut: () -> Unit) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        profile = runCatching { repository.me() }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        val current = profile
        if (current == null) {
            CircularProgressIndicator()
        } else {
            Text(current.fullName, style = MaterialTheme.typography.titleLarge)
            Text("\$${current.cashtag}")
            Text(current.email)
            Text("Identity verification: ${current.kycStatus}")
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                authViewModel.logout()
                onLoggedOut()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Log out") }
    }
}
