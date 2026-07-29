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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import week11.st560151.finalproject.util.BiometricAuthenticator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    activity: FragmentActivity,
    onBackClick: () -> Unit,
    onSettlementCompleted: () -> Unit
) {
    var payerName by remember {
        mutableStateOf("")
    }

    var receiverName by remember {
        mutableStateOf("")
    }

    var amount by remember {
        mutableStateOf("")
    }

    var isAuthenticating by remember {
        mutableStateOf(false)
    }

    var showSuccessDialog by remember {
        mutableStateOf(false)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    var snackbarMessage by remember {
        mutableStateOf<String?>(null)
    }

    val biometricAuthenticator = remember(activity) {
        BiometricAuthenticator(activity)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settle Up")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Enter the settlement information and verify your identity.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            OutlinedTextField(
                value = payerName,
                onValueChange = {
                    payerName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Payer")
                },
                placeholder = {
                    Text("Enter payer name")
                },
                singleLine = true,
                enabled = !isAuthenticating
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            OutlinedTextField(
                value = receiverName,
                onValueChange = {
                    receiverName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Receiver")
                },
                placeholder = {
                    Text("Enter receiver name")
                },
                singleLine = true,
                enabled = !isAuthenticating
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    amount = newValue.filter { character ->
                        character.isDigit() ||
                                character == '.'
                    }
                },
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                enabled = !isAuthenticating
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription =
                        "Fingerprint authentication",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Text(
                    text = "Biometric confirmation is required before settlement.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = {
                    val amountNumber =
                        amount.toDoubleOrNull()

                    when {
                        payerName.isBlank() -> {
                            snackbarMessage =
                                "Please enter the payer name."
                        }

                        receiverName.isBlank() -> {
                            snackbarMessage =
                                "Please enter the receiver name."
                        }

                        payerName.trim().equals(
                            receiverName.trim(),
                            ignoreCase = true
                        ) -> {
                            snackbarMessage =
                                "Payer and receiver cannot be the same."
                        }

                        amountNumber == null ||
                                amountNumber <= 0.0 -> {
                            snackbarMessage =
                                "Please enter a valid amount."
                        }

                        else -> {
                            isAuthenticating = true

                            biometricAuthenticator.authenticate {
                                    result ->

                                isAuthenticating = false

                                when (result) {
                                    BiometricAuthenticator.Result.Success -> {
                                        showSuccessDialog = true
                                    }

                                    BiometricAuthenticator.Result.Failed -> {
                                        snackbarMessage =
                                            "Authentication failed. Try again."
                                    }

                                    BiometricAuthenticator.Result.NotEnrolled -> {
                                        snackbarMessage =
                                            "No biometric or device lock is enrolled."
                                    }

                                    BiometricAuthenticator.Result.NotAvailable -> {
                                        snackbarMessage =
                                            "Biometric authentication is unavailable."
                                    }

                                    is BiometricAuthenticator.Result.Error -> {
                                        snackbarMessage =
                                            result.message
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !isAuthenticating
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("Confirm Settlement")
                }
            }

            snackbarMessage?.let { message ->
                androidx.compose.runtime.LaunchedEffect(message) {
                    snackbarHostState.showSnackbar(message)
                    snackbarMessage = null
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Settlement Completed")
            },
            text = {
                Text(
                    "$payerName paid $receiverName " +
                            "$${formatAmount(amount)}."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onSettlementCompleted()
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}

private fun formatAmount(
    amount: String
): String {
    val number = amount.toDoubleOrNull() ?: 0.0

    return String.format(
        java.util.Locale.US,
        "%.2f",
        number
    )
}