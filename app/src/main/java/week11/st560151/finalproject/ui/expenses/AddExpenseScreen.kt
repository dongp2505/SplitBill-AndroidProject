package week11.st560151.finalproject.ui.expenses

import androidx.compose.animation.AnimatedContent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.ui.components.CircleBackButton
import week11.st560151.finalproject.ui.components.ErrorText
import week11.st560151.finalproject.ui.components.PrimaryButton
import week11.st560151.finalproject.ui.components.SecondaryButton
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.ui.theme.CategorySelected
import week11.st560151.finalproject.ui.theme.ParticipantSelected
import week11.st560151.finalproject.viewmodel.ExpenseViewModel
import week11.st560151.finalproject.viewmodel.GroupViewModel
import java.io.ByteArrayOutputStream

private const val RECEIPT_MAX_DIMENSION = 800
private const val RECEIPT_JPEG_QUALITY = 55

private fun receiptUriToCompressedBase64(context: Context, uri: Uri): String? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val original = BitmapFactory.decodeStream(inputStream) ?: return null
    inputStream.close()

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

    val outputStream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, RECEIPT_JPEG_QUALITY, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

private fun receiptBase64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

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
    val context = LocalContext.current

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
    var receiptBase64 by remember { mutableStateOf<String?>(null) }
    var isReceiptEnlarged by remember { mutableStateOf(false) }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            receiptUriToCompressedBase64(context, it)?.let { encoded ->
                receiptBase64 = encoded
            }
        }
    }

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

            // Borderless, large-type amount entry, set apart from the fields below.
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

            AnimatedContent(targetState = isEqualSplit, label = "splitMode") { equalSplit ->
                if (equalSplit) {
                    val count = selectedParticipantIds.size.coerceAtLeast(1)
                    val each = amount / count

                    Text(
                        text = "Split %d ways · $%.2f each".format(count, each),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
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
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Receipt (optional)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val receiptBitmap = remember(receiptBase64) {
                receiptBase64?.let(::receiptBase64ToBitmap)
            }

            AnimatedContent(targetState = receiptBitmap, label = "addExpenseReceipt") { bitmap ->
                if (bitmap == null) {
                    SecondaryButton(
                        text = "Add receipt photo",
                        onClick = {
                            receiptPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Receipt photo",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isReceiptEnlarged = true },
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            TextButton(onClick = { receiptBase64 = null }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
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
                                        bitmap = bitmap.asImageBitmap(),
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
                    }
                }
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
                    shares = shares,
                    receiptBase64 = receiptBase64.orEmpty()
                )
            }
        )
    }
}