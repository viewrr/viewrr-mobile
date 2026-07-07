package com.makd.afinity.shared.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Payments opt-in section (p2p-0020 / #3). Embedded into [com.makd.afinity.shared.ui.settings.SettingsScreen]
 * rather than a separate nav destination — matches this app's single-page settings convention.
 *
 * Default OFF; toggling on is the ONLY thing that triggers a Hub wallet call. Read-only: shows
 * the derived EVM address + USDC balance, no send/open-channel actions (legal #9 open).
 */
@Composable
fun PaymentsScreen(viewModel: PaymentsViewModel = koinViewModel()) {
    val enabled by viewModel.paymentsEnabled.collectAsState()
    val walletState by viewModel.walletState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = { Text("Enable payments", style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Text(
                    "Shows your wallet address and USDC balance. Off by default.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(checked = enabled, onCheckedChange = viewModel::setPaymentsEnabled)
            },
        )

        when (val state = walletState) {
            is WalletUiState.Hidden -> Unit
            is WalletUiState.Loading -> WalletCard {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }

            is WalletUiState.Loaded -> WalletCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Wallet address",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.address,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = "${state.usdcBalance} USDC",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is WalletUiState.Error -> WalletCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::retry) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun WalletCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp)) { content() }
    }
}
