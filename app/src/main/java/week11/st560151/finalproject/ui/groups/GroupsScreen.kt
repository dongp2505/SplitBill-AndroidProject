package week11.st560151.finalproject.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBackClick: () -> Unit,
    groupViewModel: GroupViewModel = viewModel()
) {
    val groupsState by
    groupViewModel.groupsState.collectAsState()

    val createState by
    groupViewModel.createState.collectAsState()

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var groupName by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        groupViewModel.observeGroups()
    }

    LaunchedEffect(createState) {
        if (createState is UiState.Success) {
            groupName = ""
            showCreateDialog = false
            groupViewModel.resetCreateState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Groups")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showCreateDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription =
                        "Create group"
                )
            }
        }
    ) { paddingValues ->

        when (val state = groupsState) {
            UiState.Idle,
            UiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Text(
                    text = state.message,
                    color =
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(24.dp)
                )
            }

            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Text(
                        text =
                            "No groups yet. Press + to create one.",
                        modifier = Modifier
                            .padding(paddingValues)
                            .padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.data,
                            key = {
                                it.id
                            }
                        ) { group ->
                            GroupCard(group)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (
                    createState !is UiState.Loading
                ) {
                    showCreateDialog = false
                }
            },
            title = {
                Text("Create Group")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                        },
                        label = {
                            Text("Group Name")
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    if (
                        createState is UiState.Error
                    ) {
                        Text(
                            text =
                                (createState
                                        as UiState.Error)
                                    .message,
                            color =
                                MaterialTheme
                                    .colorScheme.error,
                            modifier =
                                Modifier.padding(
                                    top = 8.dp
                                )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupViewModel.createGroup(
                            groupName
                        )
                    },
                    enabled =
                        createState !is UiState.Loading
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        groupViewModel
                            .resetCreateState()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GroupCard(
    group: Group
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = group.name,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.padding(3.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text =
                        "${group.memberIds.size} member(s)"
                )

                Text(
                    text =
                        "Code: ${group.inviteCode}"
                )
            }
        }
    }
}