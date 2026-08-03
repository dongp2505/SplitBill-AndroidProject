package week11.st560151.finalproject.ui.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.ui.components.AppBottomNav
import week11.st560151.finalproject.ui.components.AvatarChip
import week11.st560151.finalproject.ui.components.GroupAvatar
import week11.st560151.finalproject.ui.components.BottomNavTab
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.ui.theme.CardBackground
import week11.st560151.finalproject.ui.theme.CardBorder
import week11.st560151.finalproject.ui.theme.FabGreen
import week11.st560151.finalproject.ui.theme.TagBackground
import week11.st560151.finalproject.ui.theme.TagText
import week11.st560151.finalproject.viewmodel.GroupViewModel
import week11.st560151.finalproject.viewmodel.NotificationViewModel
import java.util.Locale

@Composable
fun GroupsScreen(
    onGroupClick: (String) -> Unit,
    onAddGroupClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    groupViewModel: GroupViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val groupsState by groupViewModel.groupsState.collectAsState()
    val balancesState by groupViewModel.balancesState.collectAsState()
    val membersByGroupState by groupViewModel.membersByGroupState.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()

    val unreadNotificationCount = notifications.count { notification ->
        !notification.read
    }

    LaunchedEffect(Unit) {
        groupViewModel.observeGroups()
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val initial = (currentUser?.displayName?.firstOrNull()
        ?: currentUser?.email?.firstOrNull()
        ?: '?').uppercaseChar()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGroupClick,
                shape = CircleShape,
                containerColor = FabGreen,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add group")
            }
        },
        bottomBar = {
            AppBottomNav(
                selected = BottomNavTab.Groups,
                unreadNotificationCount = unreadNotificationCount,
                onSelect = { tab ->
                    when (tab) {
                        BottomNavTab.Groups -> Unit
                        BottomNavTab.Notifications -> onNotificationsClick()
                        BottomNavTab.Profile -> onProfileClick()
                    }
                }
            )
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your groups",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = initial.toString(), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (val state = groupsState) {
                UiState.Idle,
                UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = "No groups yet. Tap + to create or join one.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = state.data, key = { it.id }) { group ->
                                GroupCard(
                                    group = group,
                                    members = membersByGroupState[group.id].orEmpty(),
                                    currentUserId = currentUser?.uid.orEmpty(),
                                    balance = balancesState[group.id],
                                    onClick = { onGroupClick(group.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: Group,
    members: List<User>,
    currentUserId: String,
    balance: Double?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupAvatar(group = group, size = 36.dp)

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(text = group.name, fontWeight = FontWeight.Bold)
                }

                if (group.type.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(TagBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = group.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = TagText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    members.forEach { user ->
                        AvatarChip(user = user, borderColor = CardBackground)
                    }
                }

                BalanceLabel(currentUserId = currentUserId, balance = balance)
            }
        }
    }
}

@Composable
private fun BalanceLabel(currentUserId: String, balance: Double?) {
    when {
        balance == null -> Unit

        balance > 0.01 -> {
            Text(
                text = "You are owed ${formatMoney(balance)}",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        balance < -0.01 -> {
            Text(
                text = "You owe ${formatMoney(-balance)}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        else -> {
            Text(
                text = "Settled up",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatMoney(amount: Double): String {
    return String.format(Locale.US, "$%.2f", amount)
}