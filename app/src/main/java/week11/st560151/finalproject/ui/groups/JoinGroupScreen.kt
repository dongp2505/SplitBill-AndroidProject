package week11.st560151.finalproject.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import week11.st560151.finalproject.ui.components.AppTextField
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.components.PrimaryButton
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.viewmodel.GroupViewModel

@Composable
fun JoinGroupScreen(
    onBackClick: () -> Unit,
    onGroupJoined: () -> Unit,
    groupViewModel: GroupViewModel = viewModel()
) {
    var inviteCode by remember { mutableStateOf("") }

    val joinState by groupViewModel.joinState.collectAsState()

    LaunchedEffect(joinState) {
        if (joinState is UiState.Success) {
            groupViewModel.resetJoinState()
            onGroupJoined()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            CircleBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Join group",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Enter the invite code someone sent you",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            AppTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase() },
                label = "Invite code",
                placeholder = "e.g. 7XGM-KDHK",
                enabled = joinState !is UiState.Loading
            )

            if (joinState is UiState.Error) {
                ErrorText(message = (joinState as UiState.Error).message)
            }
        }

        PrimaryButton(
            text = "Join Group",
            isLoading = joinState is UiState.Loading,
            onClick = {
                groupViewModel.joinGroup(inviteCode)
            }
        )
    }
}
