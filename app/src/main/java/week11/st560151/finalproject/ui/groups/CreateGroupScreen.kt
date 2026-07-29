package week11.st560151.finalproject.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import week11.st560151.finalproject.ui.components.AppTextField
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.components.PrimaryButton
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.viewmodel.GroupViewModel

private val GROUP_TYPES = listOf("Roommates", "Travel", "Friends")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    onBackClick: () -> Unit,
    onGroupCreated: (String) -> Unit,
    groupViewModel: GroupViewModel = viewModel()
) {
    var groupName by remember { mutableStateOf("") }
    var groupType by remember { mutableStateOf(GROUP_TYPES.first()) }
    var inviteEmails by remember { mutableStateOf("") }

    val createState by groupViewModel.createState.collectAsState()

    LaunchedEffect(createState) {
        val state = createState
        if (state is UiState.Success) {
            groupViewModel.resetCreateState()
            onGroupCreated(state.data)
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
            CircleBackButton(onClick = onBackClick)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Create group",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            AppTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = "Group name",
                placeholder = "e.g. Lake House Weekend",
                enabled = createState !is UiState.Loading
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Type", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GROUP_TYPES.forEach { type ->
                    FilterChip(
                        selected = groupType == type,
                        onClick = { groupType = type },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AppTextField(
                value = inviteEmails,
                onValueChange = { inviteEmails = it },
                label = "Invite members",
                placeholder = "Comma-separated emails",
                enabled = createState !is UiState.Loading
            )

            Text(
                text = "Only matches existing SplitBill accounts — " +
                    "anyone else can join later with the invite code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (createState is UiState.Error) {
                ErrorText(message = (createState as UiState.Error).message)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Create Group",
            isLoading = createState is UiState.Loading,
            onClick = {
                groupViewModel.createGroup(
                    name = groupName,
                    type = groupType,
                    inviteEmails = inviteEmails.split(",")
                )
            }
        )
    }
}
