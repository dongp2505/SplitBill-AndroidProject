package week11.st560151.finalproject.ui.settlements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import week11.st560151.finalproject.util.BiometricAuthenticator
import week11.st560151.finalproject.viewmodel.SettlementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    activity: FragmentActivity,
    onBackClick: () -> Unit,
    onSettlementCompleted: () -> Unit,
    settlementViewModel: SettlementViewModel =
        viewModel()
) {
    val state by
    settlementViewModel.uiState.collectAsState()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val biometricAuthenticator =
        remember(activity) {
            BiometricAuthenticator(activity)
        }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            settlementViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settle Up",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        enabled = !state.isSaving
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Settlement Details",
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "Choose the registered payer and receiver using their email addresses.",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text =
                    "Confirmed by: ${state.createdByName}",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = state.payerEmail,
                onValueChange =
                    settlementViewModel::updatePayerEmail,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Payer Email")
                },
                placeholder = {
                    Text("payer@email.com")
                },
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            OutlinedTextField(
                value = state.receiverEmail,
                onValueChange =
                    settlementViewModel::updateReceiverEmail,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Receiver Email")
                },
                placeholder = {
                    Text("receiver@email.com")
                },
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange =
                    settlementViewModel::updateAmount,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Amount")
                },
                placeholder = {
                    Text("0.00")
                },
                prefix = {
                    Text("$")
                },
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        Icons.Filled.Fingerprint,
                    contentDescription =
                        "Biometric authentication",
                    tint =
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Text(
                    text =
                        "The signed-in user must confirm this settlement using biometric authentication.",
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = {
                    if (!settlementViewModel.validate()) {
                        return@Button
                    }

                    biometricAuthenticator.authenticate { result ->

                        when (result) {
                            BiometricAuthenticator
                                .Result.Success -> {

                                settlementViewModel
                                    .completeSettlement()
                            }

                            BiometricAuthenticator
                                .Result.Failed -> {

                                settlementViewModel.showError(
                                    "Authentication failed. Please try again."
                                )
                            }

                            BiometricAuthenticator
                                .Result.NotEnrolled -> {

                                settlementViewModel.showError(
                                    "No biometric or device lock is enrolled."
                                )
                            }

                            BiometricAuthenticator
                                .Result.NotAvailable -> {

                                settlementViewModel.showError(
                                    "Biometric authentication is unavailable."
                                )
                            }

                            is BiometricAuthenticator
                            .Result.Error -> {

                                settlementViewModel.showError(
                                    result.message
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color =
                            MaterialTheme.colorScheme
                                .onPrimary
                    )
                } else {
                    Icon(
                        imageVector =
                            Icons.Filled.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("Confirm Settlement")
                }
            }
        }
    }

    if (state.isCompleted) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Settlement Completed")
            },
            text = {
                Text(
                    text =
                        "The selected payer and receiver were saved to Firebase."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settlementViewModel.clearForm()
                        onSettlementCompleted()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}