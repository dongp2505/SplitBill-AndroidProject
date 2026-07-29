package week11.st560151.finalproject.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.components.PrimaryButton
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.ui.theme.CategorySelected
import week11.st560151.finalproject.ui.theme.ParticipantSelected
import week11.st560151.finalproject.viewmodel.ExpenseViewModel
import week11.st560151.finalproject.viewmodel.GroupViewModel

private val CATEGORIES = listOf(
    "Food", "Rent", "Utilities", "Transport", "Fun", "Other"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    groupId: String,
    onBackClick: () -> Unit,
    onExpenseSaved: () -> Unit,
    expenseViewModel: ExpenseViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val groupState by groupViewModel.groupState.collectAsState()
    val membersState by expenseViewModel.membersState.collectAsState()
    val saveState by expenseViewModel.saveState.collectAsState()

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var paidBy by remember { mutableStateOf(currentUserId ?: "") }
    var isEqualSplit by remember { mutableStateOf(true) }
    var selectedParticipantIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customAmounts by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(groupId) {
        groupViewModel.loadGroup(groupId)
    }

    LaunchedEffect(groupState) {
        val group = (groupState as? UiState.Success<Group>)?.data
        if (group != null) {
            expenseViewModel.loadMembers(group.memberIds)
            selectedParticipantIds = group.memberIds.toSet()
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) {
            expenseViewModel.resetSaveState()
            onExpenseSaved()
        }
    }

    val members = (membersState as? UiState.Success<List<User>>)?.data ?: emptyList()
    val amount = amountText.toDoubleOrNull() ?: 0.0

    fun nameFor(user: User): String {
        return if (user.uid == currentUserId) "You" else user.displayName.ifBlank { user.email }
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
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleBackButton(onClick = onBackClick)

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Add expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Borderless, large-type amount entry — sits apart from the
            // bordered fields below it, matching the wireframe.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = value
                        }
                    },
                    placeholder = {
                        Text(
                            text = "0.00",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    textStyle = TextStyle(fontSize = 32.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What was it for?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Category", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CategorySelected,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Paid by", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (membersState is UiState.Loading) {
                Text(
                    text = "Loading group members…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { user ->
                        FilterChip(
                            selected = paidBy == user.uid,
                            onClick = { paidBy = user.uid },
                            label = { Text(nameFor(user)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ParticipantSelected,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Split", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isEqualSplit,
                    onClick = { isEqualSplit = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Equally")
                }

                SegmentedButton(
                    selected = !isEqualSplit,
                    onClick = { isEqualSplit = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Custom")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { user ->
                    FilterChip(
                        selected = selectedParticipantIds.contains(user.uid),
                        onClick = {
                            selectedParticipantIds = if (selectedParticipantIds.contains(user.uid)) {
                                selectedParticipantIds - user.uid
                            } else {
                                selectedParticipantIds + user.uid
                            }
                        },
                        label = { Text(nameFor(user)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ParticipantSelected,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEqualSplit) {
                val count = selectedParticipantIds.size.coerceAtLeast(1)
                val each = amount / count

                Text(
                    text = "Split %d ways · $%.2f each".format(count, each),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                members
                    .filter { selectedParticipantIds.contains(it.uid) }
                    .forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(nameFor(user))

                            OutlinedTextField(
                                value = customAmounts[user.uid] ?: "",
                                onValueChange = { value ->
                                    customAmounts = customAmounts + (user.uid to value)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }

                val customTotal = customAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Custom total: $%.2f of $%.2f".format(customTotal, amount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (saveState is UiState.Error) {
                ErrorText(message = (saveState as UiState.Error).message)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Add expense",
            isLoading = saveState is UiState.Loading,
            onClick = {
                val shares: Map<String, Double> = if (isEqualSplit) {
                    val count = selectedParticipantIds.size.coerceAtLeast(1)
                    selectedParticipantIds.associateWith { amount / count }
                } else {
                    selectedParticipantIds.associateWith { uid ->
                        customAmounts[uid]?.toDoubleOrNull() ?: 0.0
                    }
                }

                expenseViewModel.createExpense(
                    groupId = groupId,
                    description = description,
                    amount = amount,
                    category = category,
                    paidBy = paidBy,
                    participantIds = selectedParticipantIds.toList(),
                    shares = shares
                )
            }
        )
    }
}
