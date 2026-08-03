package week11.st560151.finalproject.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import week11.st560151.finalproject.R
import week11.st560151.finalproject.ui.components.AppPasswordField
import week11.st560151.finalproject.ui.components.AppTextField
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.components.InlineLink
import week11.st560151.finalproject.ui.components.PrimaryButton
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    val state by authViewModel.loginState.collectAsState()

    LaunchedEffect(state) {
        if (state is UiState.Success) {
            authViewModel.resetLoginState()
            onLoginSuccess()
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
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sign in to see your groups",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "you@example.com",
                enabled = state !is UiState.Loading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppPasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Your password",
                enabled = state !is UiState.Loading
            )

            if (state is UiState.Error) {
                ErrorText(
                    message = (state as UiState.Error).message
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            InlineLink(
                text = "Forgot password?",
                onClick = onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.illustration_team),
                contentDescription = "Team helping each other reach shared goals",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            text = "Sign in",
            onClick = {
                authViewModel.login(
                    email = email,
                    password = password
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
                text = "Don't have an account? ",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            InlineLink(
                text = "Create one",
                onClick = onRegisterClick
            )
        }
    }
}
