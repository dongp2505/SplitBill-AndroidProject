package week11.st560151.finalproject.ui.settlements

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import week11.st560151.finalproject.R
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.ui.components.AvatarChip
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.PrimaryButton
import week11.st560151.finalproject.ui.theme.BiometricCircle
import week11.st560151.finalproject.ui.theme.CardBackground
import week11.st560151.finalproject.ui.theme.CardBorder
import week11.st560151.finalproject.ui.theme.ConfirmedCircle
import week11.st560151.finalproject.util.BiometricAuthenticator
import week11.st560151.finalproject.viewmodel.SettlementUiState
import week11.st560151.finalproject.viewmodel.SettlementViewModel

private enum class SettlementStep {
    Info,
    Biometric,
    Confirmed
}

@Composable
fun SettlementScreen(
    activity: FragmentActivity,
    onBackClick: () -> Unit,
    onSettlementCompleted: () -> Unit,
    settlementViewModel: SettlementViewModel = viewModel()
) {
    val state by settlementViewModel.uiState.collectAsState()

    var step by remember {
        mutableStateOf(SettlementStep.Info)
    }

    val biometricAuthenticator = remember(activity) {
        BiometricAuthenticator(activity)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            settlementViewModel.clearError()
        }
    }

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            step = SettlementStep.Confirmed
        }
    }

    val payerName = displayName(state.payerUser)

    // Disabled mid-save so an in-flight save can't be backed out of.
    BackHandler(
        enabled = step == SettlementStep.Biometric &&
                !state.isSaving
    ) {
        step = SettlementStep.Info
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier.padding(paddingValues)
        ) {
            when (step) {
                SettlementStep.Info -> {
                    InfoStep(
                        state = state,
                        payerName = payerName,
                        onBackClick = onBackClick,
                        onRequestSettlement = {
                            if (settlementViewModel.validate()) {
                                step = SettlementStep.Biometric
                            }
                        }
                    )
                }

                SettlementStep.Biometric -> {
                    BiometricStep(
                        payerName = payerName,
                        amount = state.amount,
                        isSaving = state.isSaving,

                        onBackClick = {
                            if (!state.isSaving) {
                                step = SettlementStep.Info
                            }
                        },

                        onScanClick = {
                            biometricAuthenticator.authenticate { result ->

                                when (result) {
                                    BiometricAuthenticator.Result.Success -> {
                                        settlementViewModel.completeSettlement()
                                    }

                                    BiometricAuthenticator.Result.Failed -> {
                                        settlementViewModel.showError(
                                            "Authentication failed. Please try again."
                                        )
                                    }

                                    BiometricAuthenticator.Result.NotEnrolled -> {
                                        settlementViewModel.showError(
                                            "No fingerprint, face scan, PIN, pattern, or password is enrolled."
                                        )
                                    }

                                    BiometricAuthenticator.Result.NotAvailable -> {
                                        settlementViewModel.showError(
                                            "Biometric authentication is unavailable on this device."
                                        )
                                    }

                                    is BiometricAuthenticator.Result.Error -> {
                                        settlementViewModel.showError(
                                            result.message
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                SettlementStep.Confirmed -> {
                    ConfirmedStep(
                        payerName = payerName,
                        onDoneClick = {
                            settlementViewModel.clearForm()
                            onSettlementCompleted()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoStep(
    state: SettlementUiState,
    payerName: String,
    onBackClick: () -> Unit,
    onRequestSettlement: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .padding(24.dp)
    ) {
        CircleBackButton(
            onClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            if (state.isLoadingParticipants) {
                Text(
                    text = "Loading…",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            } else {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy((-10).dp)
                ) {
                    state.payerUser?.let { payer ->
                        AvatarChip(
                            user = payer,
                            size = 56.dp,
                            borderColor =
                                MaterialTheme.colorScheme
                                    .background
                        )
                    }

                    state.receiverUser?.let { receiver ->
                        AvatarChip(
                            user = receiver,
                            size = 56.dp,
                            borderColor =
                                MaterialTheme.colorScheme
                                    .background
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "$${state.amount}",
                    style =
                        MaterialTheme.typography
                            .headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "$payerName owes you",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .background(CardBackground)
                        .border(
                            width = 1.dp,
                            color = CardBorder,
                            shape =
                                RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text =
                            "Confirm that you settled this payment outside the app. " +
                                    "You must verify your identity before the settlement is saved.",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        style =
                            MaterialTheme.typography
                                .bodyMedium
                    )
                }
            }
        }

        PrimaryButton(
            text = "Request Settlement",
            enabled =
                !state.isLoadingParticipants &&
                        state.payerUser != null &&
                        state.receiverUser != null &&
                        state.amount.isNotBlank(),
            onClick = onRequestSettlement
        )
    }
}

@Composable
private fun BiometricStep(
    payerName: String,
    amount: String,
    isSaving: Boolean,
    onBackClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .padding(24.dp)
    ) {
        CircleBackButton(
            onClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            PulsingFingerprint()

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Confirm on your device",
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "Touch the fingerprint sensor to verify $$amount\n" +
                            "$payerName owes you",
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        PrimaryButton(
            text = "Scan fingerprint",
            enabled = !isSaving,
            isLoading = isSaving,
            onClick = onScanClick
        )
    }
}

@Composable
private fun ConfirmedStep(
    payerName: String,
    onDoneClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(ConfirmedCircle),
                contentAlignment = Alignment.Center
            ) {
                BouncingDots()
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "You're confirmed",
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "The settlement with $payerName was saved successfully.",
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        PrimaryButton(
            text = "Done",
            onClick = onDoneClick
        )
    }
}

@Composable
private fun PulsingFingerprint() {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "fingerprint_pulse"
        )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fingerprint_scale"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(BiometricCircle),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.ic_fingerprint
            ),
            contentDescription =
                "Fingerprint verification",
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun BouncingDots() {
    val transition =
        rememberInfiniteTransition(
            label = "bouncing_dots"
        )

    Row(
        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->

            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400,
                        delayMillis = index * 150,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )

            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFF9E9E9E)
                    )
            )
        }
    }
}

private fun displayName(
    user: User?
): String {
    if (user == null) {
        return "Member"
    }

    return user.displayName.ifBlank {
        user.email.substringBefore("@")
    }
}