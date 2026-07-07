package com.makd.afinity.shared.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Self-custody identity onboarding (#142 rung 3). One screen, driven by [OnboardingViewModel]'s
 * step state machine: display the mnemonic once -> confirm 3 words -> set a master password -> done.
 * Matches [com.makd.afinity.shared.ui.auth.LoginScreen] conventions and [ViewrrTheme].
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val step = state.step) {
                is OnboardingStep.DisplayMnemonic -> DisplayMnemonicStep(
                    words = step.words,
                    onContinue = viewModel::confirmWritten,
                    onBack = onBack,
                )

                is OnboardingStep.Confirm -> ConfirmStep(
                    positions = step.positions,
                    error = state.error,
                    onSubmit = viewModel::submitConfirmation,
                    onBack = viewModel::backToMnemonic,
                )

                is OnboardingStep.SetPassword -> SetPasswordStep(
                    error = state.error,
                    busy = state.busy,
                    onSubmit = viewModel::setPassword,
                )

                is OnboardingStep.Done -> DoneStep(handle = step.handle, onDone = onDone)
            }
        }
    }
}

@Composable
private fun DisplayMnemonicStep(
    words: List<String>,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = "Your recovery phrase",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    )
    Text(
        text = "Write these 12 words down and keep them safe. This is shown once — anyone with " +
            "these words controls your identity, and we can't recover them for you.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            words.forEachIndexed { index, word ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(width = 32.dp, height = 20.dp),
                    )
                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text("I've written it down")
    }
    TextButton(onClick = onBack) { Text("Cancel") }
}

@Composable
private fun ConfirmStep(
    positions: List<Int>,
    error: String?,
    onSubmit: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val answers = remember(positions) { mutableStateListOf(*Array(positions.size) { "" }) }

    Text(
        text = "Confirm your backup",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    )
    Text(
        text = "Enter the following words from your recovery phrase to confirm you saved it.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    positions.forEachIndexed { index, position ->
        OutlinedTextField(
            value = answers[index],
            onValueChange = { answers[index] = it },
            label = { Text("Word #$position") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(onClick = { onSubmit(answers.toList()) }, modifier = Modifier.fillMaxWidth()) {
        Text("Confirm")
    }
    TextButton(onClick = onBack) { Text("Show my recovery phrase again") }
}

@Composable
private fun SetPasswordStep(
    error: String?,
    busy: Boolean,
    onSubmit: (String, String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Text(
        text = "Set a master password",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    )
    Text(
        text = "This password encrypts your identity on this device. It never leaves your device " +
            "and can't be reset — choose something you'll remember.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Master password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = confirm,
        onValueChange = { confirm = it },
        label = { Text("Confirm password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(
        onClick = { onSubmit(password, confirm) },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Text("Create identity")
        }
    }
}

@Composable
private fun DoneStep(handle: String, onDone: () -> Unit) {
    Text(
        text = "Identity created",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    )
    Text(
        text = "Your self-custody identity is encrypted and ready on this device.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your identity",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = handle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text("Done")
    }
}
