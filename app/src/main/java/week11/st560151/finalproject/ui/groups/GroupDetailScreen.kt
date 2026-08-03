package week11.st560151.finalproject.ui.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import week11.st560151.finalproject.data.model.Expense
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.ui.components.AvatarChip
import week11.st560151.finalproject.ui.components.GroupAvatar
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.ui.theme.CardBackground
import week11.st560151.finalproject.ui.theme.CardBorder
import week11.st560151.finalproject.ui.theme.CategorySelected
import week11.st560151.finalproject.ui.theme.DividerGray
import week11.st560151.finalproject.ui.theme.FabGreen
import week11.st560151.finalproject.util.BalanceCalculator
import week11.st560151.finalproject.viewmodel.ExpenseViewModel
import week11.st560151.finalproject.viewmodel.GroupViewModel
import java.util.Locale
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream

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
    val editGroupState by groupViewModel.editGroupState.collectAsState()

    var expenseBeingEdited by remember { mutableStateOf<Expense?>(null) }
    var selectedTab by remember { mutableStateOf(GroupTab.Activity) }
    var showEditGroupDialog by remember { mutableStateOf(false) }

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupAvatar(group = group, size = 40.dp)

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        members.take(4).forEach { user ->
                            AvatarChip(user = user, borderColor = MaterialTheme.colorScheme.background)
                        }
                    }

                    if (group != null && currentUserId in group.memberIds) {
                        IconButton(onClick = { showEditGroupDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit group")
                        }
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

    if (showEditGroupDialog && group != null) {
        EditGroupDialog(
            group = group,
            members = members,
            editState = editGroupState,
            currentUserId = currentUserId,
            onDismiss = {
                showEditGroupDialog = false
                groupViewModel.resetEditGroupState()
            },
            onSaveName = { groupViewModel.updateGroupName(group.id, it) },
            onSaveAvatar = { groupViewModel.updateGroupAvatar(group.id, it) },
            onAddMember = { groupViewModel.addGroupMember(group.id, it) },
            onRemoveMember = { groupViewModel.removeGroupMember(group.id, it) },
            onLeaveGroup = {
                groupViewModel.removeGroupMember(group.id, currentUserId)
                showEditGroupDialog = false
                onBackClick()
            }
        )
    }

    expenseBeingEdited?.let { expense ->
        ExpenseDetailsDialog(
            expense = expense,

            // Only the original creator may edit or delete.
            canEdit = expense.createdBy == currentUserId,

            isDeleting = deleteState is UiState.Loading,

            onDismiss = {
                expenseBeingEdited = null
            },

            onSave = { updatedExpense ->
                expenseViewModel.updateExpense(updatedExpense)
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
        ErrorText(
            message = balancesState.message
        )
        return
    }

    val balance =
        (
                balancesState as?
                        UiState.Success<Map<String, Double>>
                )
            ?.data
            ?.get(currentUserId)

    when {
        balance == null -> {
            Text(
                text = "Loading…",
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }

        balance > 0.0 -> {
            Text(
                text =
                    "You are owed " +
                            "+${formatMoney(balance)}",
                color =
                    MaterialTheme.colorScheme
                        .tertiary,
                fontWeight = FontWeight.Bold,
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
        }

        balance < 0.0 -> {
            Text(
                text =
                    "You owe " +
                            "-${formatMoney(-balance)}",
                color =
                    MaterialTheme.colorScheme
                        .error,
                fontWeight = FontWeight.Bold,
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
        }

        else -> {
            Text(
                text = "Balance ${formatMoney(0.0)}",
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                style =
                    MaterialTheme.typography
                        .titleMedium
            )
        }
    }
}

private enum class ExpenseSortOption(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    AMOUNT_DESC("Amount: high to low"),
    AMOUNT_ASC("Amount: low to high")
}

@Composable
private fun ActivityTab(
    expensesState: UiState<List<Expense>>,
    onExpenseClick: (Expense) -> Unit
) {
    when (expensesState) {
        UiState.Idle,
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        is UiState.Error -> {
            ErrorText(message = expensesState.message)
            return
        }

        is UiState.Success -> Unit
    }

    val expenses = (expensesState as UiState.Success<List<Expense>>).data

    if (expenses.isEmpty()) {
        Text(
            text = "No expenses yet. Tap + to add one.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(ExpenseSortOption.NEWEST) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val categories = remember(expenses) {
        expenses.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredExpenses = remember(expenses, searchQuery, selectedCategory, sortOption) {
        expenses
            .filter { expense ->
                (selectedCategory == null || expense.category == selectedCategory) &&
                        (searchQuery.isBlank() || expense.description.contains(searchQuery, ignoreCase = true))
            }
            .let { list ->
                when (sortOption) {
                    ExpenseSortOption.NEWEST -> list.sortedByDescending { it.createdAt }
                    ExpenseSortOption.OLDEST -> list.sortedBy { it.createdAt }
                    ExpenseSortOption.AMOUNT_DESC -> list.sortedByDescending { it.amount }
                    ExpenseSortOption.AMOUNT_ASC -> list.sortedBy { it.amount }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search expenses") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort expenses")
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    ExpenseSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortOption = option
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (categories.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CategorySelected,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(categories) { categoryOption ->
                    FilterChip(
                        selected = selectedCategory == categoryOption,
                        onClick = {
                            selectedCategory = if (selectedCategory == categoryOption) null else categoryOption
                        },
                        label = { Text(categoryOption) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CategorySelected,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredExpenses.isEmpty()) {
            Text(
                text = "No expenses match your search.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn {
                itemsIndexed(items = filteredExpenses, key = { _, item -> item.id }) { index, expense ->
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
private fun NetPositionAmount(
    balance: Double
) {
    val displayAmount =
        when {
            balance > 0.0 -> {
                "+${formatMoney(balance)}"
            }

            balance < 0.0 -> {
                "-${formatMoney(-balance)}"
            }

            else -> {
                formatMoney(0.0)
            }
        }

    val amountColor =
        when {
            balance > 0.0 -> {
                MaterialTheme.colorScheme.tertiary
            }

            balance < 0.0 -> {
                MaterialTheme.colorScheme.error
            }

            else -> {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        }

    Text(
        text = displayAmount,
        color = amountColor,
        fontWeight = FontWeight.Bold
    )
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = expense.category,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )

                if (expense.receiptBase64.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Has receipt",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Text(text = formatMoney(expense.amount), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExpenseDetailsDialog(
    expense: Expense,
    canEdit: Boolean,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    var description by remember(expense.id) {
        mutableStateOf(expense.description)
    }

    var amountText by remember(expense.id) {
        mutableStateOf(expense.amount.toString())
    }

    var receiptBase64 by remember(expense.id) {
        mutableStateOf(expense.receiptBase64)
    }

    var isReceiptEnlarged by remember { mutableStateOf(false) }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            receiptUriToBase64(context, it)?.let { encoded -> receiptBase64 = encoded }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = if (canEdit) {
                    "Edit expense"
                } else {
                    "Price details"
                }
            )
        },

        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { newValue ->
                        if (canEdit) {
                            description = newValue
                        }
                    },
                    label = {
                        Text("Description")
                    },
                    readOnly = !canEdit,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        if (canEdit) {
                            amountText = newValue
                        }
                    },
                    label = {
                        Text("Amount")
                    },
                    readOnly = !canEdit,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Receipt",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                val receiptBitmap = remember(receiptBase64) {
                    receiptBase64.takeIf { it.isNotBlank() }?.let(::receiptBase64ToBitmap)
                }

                if (receiptBitmap != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = receiptBitmap.asImageBitmap(),
                            contentDescription = "Receipt photo",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isReceiptEnlarged = true },
                            contentScale = ContentScale.Crop
                        )

                        if (canEdit) {
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                TextButton(onClick = {
                                    receiptPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }) {
                                    Text("Replace")
                                }

                                TextButton(onClick = { receiptBase64 = "" }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    if (isReceiptEnlarged) {
                        Dialog(
                            onDismissRequest = { isReceiptEnlarged = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.9f))
                                    .clickable { isReceiptEnlarged = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = receiptBitmap.asImageBitmap(),
                                    contentDescription = "Enlarged receipt photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )

                                IconButton(
                                    onClick = { isReceiptEnlarged = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else if (canEdit) {
                    TextButton(onClick = {
                        receiptPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Text("Add receipt photo")
                    }
                } else {
                    Text(
                        text = "No receipt attached.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!canEdit) {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Only the person who added this expense can edit or delete it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },

        confirmButton = {
            if (canEdit) {
                Button(
                    onClick = {
                        val newAmount =
                            amountText.toDoubleOrNull()

                        if (
                            description.trim().isNotBlank() &&
                            newAmount != null &&
                            newAmount > 0.0
                        ) {
                            // Equal splits rescale with the new amount; custom splits don't.
                            val isEvenlySplit =
                                expense.shares.values
                                    .toSet()
                                    .size <= 1

                            val updatedShares =
                                if (
                                    isEvenlySplit &&
                                    expense.participantIds.isNotEmpty()
                                ) {
                                    val each =
                                        newAmount /
                                                expense.participantIds.size

                                    expense.participantIds
                                        .associateWith {
                                            each
                                        }
                                } else {
                                    expense.shares
                                }

                            onSave(
                                expense.copy(
                                    description =
                                        description.trim(),
                                    amount = newAmount,
                                    shares = updatedShares,
                                    receiptBase64 = receiptBase64
                                )
                            )
                        }
                    }
                ) {
                    Text("Save")
                }
            } else {
                Button(
                    onClick = onDismiss
                ) {
                    Text("Close")
                }
            }
        },

        dismissButton = {
            if (canEdit) {
                Row {
                    TextButton(
                        onClick = onDelete,
                        enabled = !isDeleting
                    ) {
                        Text(
                            text = if (isDeleting) {
                                "Deleting…"
                            } else {
                                "Delete"
                            },
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}


private const val GROUP_AVATAR_MAX = 180
private const val GROUP_AVATAR_QUALITY = 55

private fun groupUriToBase64(context: Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input) ?: return null
        input.close()
        val scale = minOf(
            GROUP_AVATAR_MAX.toFloat() / original.width,
            GROUP_AVATAR_MAX.toFloat() / original.height,
            1f
        )
        val resized = Bitmap.createScaledBitmap(
            original,
            (original.width * scale).toInt().coerceAtLeast(1),
            (original.height * scale).toInt().coerceAtLeast(1),
            true
        )
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, GROUP_AVATAR_QUALITY, output)
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}

private const val RECEIPT_MAX_DIMENSION = 800
private const val RECEIPT_JPEG_QUALITY = 55

private fun receiptUriToBase64(context: Context, uri: Uri): String? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input) ?: return null
        input.close()
        val scale = minOf(
            RECEIPT_MAX_DIMENSION.toFloat() / original.width,
            RECEIPT_MAX_DIMENSION.toFloat() / original.height,
            1f
        )
        val resized = Bitmap.createScaledBitmap(
            original,
            (original.width * scale).toInt().coerceAtLeast(1),
            (original.height * scale).toInt().coerceAtLeast(1),
            true
        )
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, RECEIPT_JPEG_QUALITY, output)
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}

private fun receiptBase64ToBitmap(value: String): Bitmap? {
    return try {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun EditGroupDialog(
    group: Group,
    members: List<User>,
    editState: UiState<String>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveAvatar: (String) -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onLeaveGroup: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(group.id, group.name) { mutableStateOf(group.name) }
    var email by remember(group.id) { mutableStateOf("") }
    var memberToRemove by remember { mutableStateOf<User?>(null) }
    val loading = editState is UiState.Loading

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            groupUriToBase64(context, it)?.let(onSaveAvatar)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Manage group") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GroupAvatar(group, 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !loading
                        ) { Text("Change avatar") }
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    )
                }
                item {
                    Button(
                        onClick = { onSaveName(name.trim()) },
                        enabled = !loading && name.trim().isNotBlank() && name.trim() != group.name,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save group name") }
                }
                item { HorizontalDivider() }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Registered user email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    )
                }
                item {
                    Button(
                        onClick = {
                            onAddMember(email.trim())
                            email = ""
                        },
                        enabled = !loading && email.trim().isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add member") }
                }
                item { HorizontalDivider() }
                items(members, key = { it.uid }) { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarChip(member, borderColor = MaterialTheme.colorScheme.background)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(member.displayName.ifBlank { member.email }, fontWeight = FontWeight.Bold)
                                if (member.uid == currentUserId) Text("You", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(
                            onClick = { memberToRemove = member },
                            enabled = !loading && group.memberIds.size > 1
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove member", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = onLeaveGroup,
                        enabled = !loading && group.memberIds.size > 1,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Leave group", color = MaterialTheme.colorScheme.error) }
                }
                when (editState) {
                    is UiState.Error -> item { ErrorText(editState.message) }
                    is UiState.Success -> item { Text(editState.data, color = MaterialTheme.colorScheme.tertiary) }
                    else -> Unit
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("Done") } }
    )

    memberToRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(if (member.uid == currentUserId) "Leave group?" else "Remove member?") },
            text = { Text(if (member.uid == currentUserId) "Remove yourself from ${group.name}?" else "Remove ${member.displayName.ifBlank { member.email }} from ${group.name}?") },
            confirmButton = {
                Button(onClick = {
                    if (member.uid == currentUserId) onLeaveGroup() else onRemoveMember(member.uid)
                    memberToRemove = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text("Cancel") } }
        )
    }
}

private fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "$%.2f", amount)
}