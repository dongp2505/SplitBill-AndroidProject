package week11.st560151.finalproject.ui.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import week11.st560151.finalproject.data.model.Expense
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.ui.components.AvatarChip
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.ui.theme.CardBackground
import week11.st560151.finalproject.ui.theme.CardBorder
import week11.st560151.finalproject.ui.theme.DividerGray
import week11.st560151.finalproject.ui.theme.FabGreen
import week11.st560151.finalproject.util.BalanceCalculator
import week11.st560151.finalproject.viewmodel.ExpenseViewModel
import week11.st560151.finalproject.viewmodel.GroupViewModel
import java.util.Locale

private enum class GroupTab { Activity, Balances }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onBackClick: () -> Unit,
    onAddExpenseClick: (String) -> Unit,
    onSettleUpClick: (
        groupId: String,
        payerEmail: String,
        receiverEmail: String,
        amount: Double
    ) -> Unit,
    groupViewModel: GroupViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel()
) {
    val groupState by groupViewModel.groupState.collectAsState()
    val expensesState by expenseViewModel.expensesState.collectAsState()
    val membersState by expenseViewModel.membersState.collectAsState()
    val balancesState by expenseViewModel.balancesState.collectAsState()
    val deleteState by expenseViewModel.deleteState.collectAsState()

    var expenseBeingEdited by remember { mutableStateOf<Expense?>(null) }
    var selectedTab by remember { mutableStateOf(GroupTab.Activity) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    LaunchedEffect(groupId) {
        groupViewModel.loadGroup(groupId)
        expenseViewModel.observeExpenses(groupId)
    }

    LaunchedEffect(groupState) {
        val group = (groupState as? UiState.Success<Group>)?.data
        if (group != null) {
            expenseViewModel.loadMembers(group.memberIds)
            expenseViewModel.loadBalances(groupId, group.memberIds)
        }
    }

    LaunchedEffect(deleteState) {
        if (deleteState is UiState.Success) {
            expenseBeingEdited = null
            expenseViewModel.resetDeleteState()
        }
    }

    val group = (groupState as? UiState.Success<Group>)?.data
    val members = (membersState as? UiState.Success<List<User>>)?.data ?: emptyList()
    val membersById = members.associateBy { it.uid }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpenseClick(groupId) },
                shape = CircleShape,
                containerColor = FabGreen,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleBackButton(onClick = onBackClick)

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = group?.name ?: "Group",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "${group?.memberIds?.size ?: 0} members",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    members.take(4).forEach { user ->
                        AvatarChip(user = user, borderColor = MaterialTheme.colorScheme.background)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Your position",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    PositionText(
                        balancesState = balancesState,
                        currentUserId = currentUserId
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == GroupTab.Activity,
                    onClick = { selectedTab = GroupTab.Activity },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Activity")
                }

                SegmentedButton(
                    selected = selectedTab == GroupTab.Balances,
                    onClick = { selectedTab = GroupTab.Balances },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Balances")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                GroupTab.Activity -> {
                    ActivityTab(
                        expensesState = expensesState,
                        onExpenseClick = { expenseBeingEdited = it }
                    )
                }

                GroupTab.Balances -> {
                    BalancesTab(
                        currentUserId = currentUserId,
                        membersById = membersById,
                        balancesState = balancesState,
                        onSettleUpClick = { debtorId, amount ->
                            val debtor = membersById[debtorId]
                            val me = membersById[currentUserId]

                            onSettleUpClick(
                                groupId,
                                debtor?.email.orEmpty(),
                                me?.email.orEmpty(),
                                amount
                            )
                        }
                    )
                }
            }
        }
    }

    expenseBeingEdited?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            isDeleting = deleteState is UiState.Loading,
            onDismiss = { expenseBeingEdited = null },
            onSave = { updated ->
                expenseViewModel.updateExpense(updated)
                expenseBeingEdited = null
            },
            onDelete = {
                expenseViewModel.deleteExpense(expense.id)
            }
        )
    }
}

@Composable
private fun PositionText(
    balancesState: UiState<Map<String, Double>>,
    currentUserId: String
) {
    if (balancesState is UiState.Error) {
        ErrorText(message = balancesState.message)
        return
    }

    val balance = (balancesState as? UiState.Success<Map<String, Double>>)
        ?.data
        ?.get(currentUserId)

    when {
        balance == null -> {
            Text(text = "Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        balance > 0.01 -> {
            Text(
                text = "You are owed ${formatMoney(balance)}",
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        balance < -0.01 -> {
            Text(
                text = "You owe ${formatMoney(-balance)}",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        else -> {
            Text(
                text = "You're settled up",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ActivityTab(
    expensesState: UiState<List<Expense>>,
    onExpenseClick: (Expense) -> Unit
) {
    when (val state = expensesState) {
        UiState.Idle,
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            ErrorText(message = state.message)
        }

        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Text(
                    text = "No expenses yet. Tap + to add one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    itemsIndexed(items = state.data, key = { _, item -> item.id }) { index, expense ->
                        if (index > 0) {
                            HorizontalDivider(color = DividerGray)
                        }

                        ExpenseRow(
                            expense = expense,
                            onClick = { onExpenseClick(expense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancesTab(
    currentUserId: String,
    membersById: Map<String, User>,
    balancesState: UiState<Map<String, Double>>,
    onSettleUpClick: (debtorId: String, amount: Double) -> Unit
) {
    when (balancesState) {
        UiState.Idle,
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        is UiState.Error -> {
            ErrorText(message = balancesState.message)
            return
        }

        is UiState.Success -> Unit
    }

    val balances = (balancesState as UiState.Success<Map<String, Double>>).data

    val suggestions = remember(balances) {
        BalanceCalculator.suggestSettlements(balances)
            .filter { it.toUserId == currentUserId }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "NET POSITIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(items = balances.entries.toList(), key = { it.key }) { (userId, balance) ->
            val user = membersById[userId]

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user != null) {
                        AvatarChip(user = user, borderColor = MaterialTheme.colorScheme.background)
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = if (userId == currentUserId) {
                            "You"
                        } else {
                            user?.displayName?.ifBlank { user.email } ?: "Member"
                        }
                    )
                }

                NetPositionAmount(balance = balance)
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SUGGESTED SETTLEMENTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(items = suggestions) { suggestion ->
                val debtor = membersById[suggestion.fromUserId]

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${debtor?.displayName?.ifBlank { debtor.email } ?: "Member"} owes you",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = formatMoney(suggestion.amount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                onSettleUpClick(suggestion.fromUserId, suggestion.amount)
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Settle up")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetPositionAmount(balance: Double) {
    when {
        balance > 0.01 -> {
            Text(
                text = "is owed ${formatMoney(balance)}",
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
        }

        balance < -0.01 -> {
            Text(
                text = "owes ${formatMoney(-balance)}",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }

        else -> {
            Text(
                text = "settled up",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = expense.description,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = expense.category,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(text = formatMoney(expense.amount), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditExpenseDialog(
    expense: Expense,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit,
    onDelete: () -> Unit
) {
    var description by remember(expense.id) { mutableStateOf(expense.description) }
    var amountText by remember(expense.id) { mutableStateOf(expense.amount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: expense.amount

                    // Equal-split shares scale with the new amount; custom
                    // splits are left as-is since only description/amount
                    // are editable from this quick dialog.
                    val isEvenlySplit = expense.shares.values.toSet().size <= 1

                    val updatedShares = if (isEvenlySplit && expense.participantIds.isNotEmpty()) {
                        val each = amount / expense.participantIds.size
                        expense.participantIds.associateWith { each }
                    } else {
                        expense.shares
                    }

                    onSave(
                        expense.copy(
                            description = description.trim(),
                            amount = amount,
                            shares = updatedShares
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete, enabled = !isDeleting) {
                    Text(
                        text = if (isDeleting) "Deleting…" else "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "$%.2f", amount)
}
