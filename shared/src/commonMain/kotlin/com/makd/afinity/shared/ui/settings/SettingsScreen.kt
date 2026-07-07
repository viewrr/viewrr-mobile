package com.makd.afinity.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makd.afinity.shared.ui.payments.PaymentsScreen
import org.koin.compose.viewmodel.koinViewModel

/** Settings screen — read-only account info + a prominent logout action. commonMain-safe Material3. */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val loggedIn by viewModel.loggedIn.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SettingsRow(label = "Account", value = if (loggedIn) "Signed in" else "Signed out")
            HorizontalDivider()
            SettingsRow(label = "Server", value = "viewrr Hub (not configured)")
            HorizontalDivider()
            SettingsRow(label = "Version", value = "0.9.3-beta")
            HorizontalDivider()
            PaymentsScreen()

            Button(
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Log out")
            }
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = { Text(label, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
