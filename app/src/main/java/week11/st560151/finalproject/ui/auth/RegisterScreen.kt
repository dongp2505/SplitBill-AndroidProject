package week11.st560151.finalproject.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import week11.st560151.finalproject.ui.components.AppPasswordField
import week11.st560151.finalproject.ui.components.AppTextField
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.components.InlineLink
import week11.st560151.finalproject.ui.components.PrimaryButton
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val state by authViewModel.registerState.collectAsState()

    LaunchedEffect(state) {
        if (state is UiState.Success) {
            authViewModel.resetRegisterState()
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            CircleBackButton(
                onClick = {
                    authViewModel.resetRegisterState()
                    onBackClick()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Create account",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Track shared costs with your crew",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            AppTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Full name",
                placeholder = "Jordan Lee",
                enabled = state !is UiState.Loading
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "you@example.com",
                enabled = state !is UiState.Loading,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppPasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "At least 6 characters",
                enabled = state !is UiState.Loading,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm password",
                placeholder = "Re-enter password",
                enabled = state !is UiState.Loading
            )

            if (state is UiState.Error) {
                ErrorText(message = (state as UiState.Error).message)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Create account",
            onClick = {
                authViewModel.register(
                    displayName = displayName.trim(),
                    email = email.trim(),
                    password = password,
                    confirmPassword = confirmPassword
                )
            },
            isLoading = state is UiState.Loading
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(
                text = "Already have an account? ",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            InlineLink(
                text = "Sign in",
                onClick = {
                    authViewModel.resetRegisterState()
                    onBackClick()
                },
                enabled = state !is UiState.Loading
            )
        }
    }
}
