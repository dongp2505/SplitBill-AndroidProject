package week11.st560151.finalproject.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import week11.st560151.finalproject.data.model.AppNotification
import week11.st560151.finalproject.ui.components.AppBottomNav
import week11.st560151.finalproject.ui.components.BottomNavTab
import week11.st560151.finalproject.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    onGroupsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationClick: (String) -> Unit,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val notifications by notificationViewModel
        .notifications
        .collectAsState()

    val errorMessage by notificationViewModel
        .errorMessage
        .collectAsState()

    Scaffold(
        bottomBar = {
            AppBottomNav(
                selected = BottomNavTab.Notifications,
                onSelect = { tab ->
                    when (tab) {
                        BottomNavTab.Groups -> {
                            onGroupsClick()
                        }

                        BottomNavTab.Notifications -> {
                            Unit
                        }

                        BottomNavTab.Profile -> {
                            onProfileClick()
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background
                )
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    top = 24.dp,
                    bottom = 18.dp
                )
            )

            /*
             * Display an error from Firestore when one occurs.
             */
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        bottom = 12.dp
                    )
                )
            }

            /*
             * Empty notification state.
             */
            if (notifications.isEmpty()) {
                EmptyNotificationsContent(
                    modifier = Modifier.weight(1f)
                )
            } else {
                /*
                 * Notification list.
                 */
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(
                        12.dp
                    )
                ) {
                    items(
                        items = notifications,
                        key = { notification ->
                            notification.id
                        }
                    ) { notification ->

                        NotificationCard(
                            notification = notification,
                            onClick = {
                                /*
                                 * Mark the selected notification as read.
                                 */
                                notificationViewModel.markAsRead(
                                    notification.id
                                )

                                /*
                                 * Open the group connected to this notification.
                                 */
                                if (notification.groupId.isNotBlank()) {
                                    onNotificationClick(
                                        notification.groupId
                                    )
                                }
                            }
                        )
                    }

                    item {
                        Spacer(
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = "No notifications yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp)
            )

            Text(
                text = "Group and settlement updates will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val containerColor =
        if (notification.isRead) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme
                .colorScheme
                .primaryContainer
                .copy(alpha = 0.45f)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation =
                if (notification.isRead) {
                    1.dp
                } else {
                    3.dp
                }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /*
             * Notification icon.
             */
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notificationIcon(
                        notification.type
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            /*
             * Notification text.
             */
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme
                        .typography
                        .titleMedium,
                    fontWeight =
                        if (notification.isRead) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Bold
                        }
                )

                Text(
                    text = notification.message,
                    style = MaterialTheme
                        .typography
                        .bodyMedium,
                    color = MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 3.dp
                    )
                )

                if (notification.createdAt > 0L) {
                    Text(
                        text = formatNotificationDate(
                            notification.createdAt
                        ),
                        style = MaterialTheme
                            .typography
                            .labelSmall,
                        color = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = 7.dp
                        )
                    )
                }
            }

            /*
             * Blue dot for unread notifications.
             */
            if (!notification.isRead) {
                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

/*
 * Selects the correct icon based on notification type.
 */
private fun notificationIcon(
    notificationType: String
): ImageVector {
    return when (notificationType) {
        "SETTLEMENT_COMPLETED" -> {
            Icons.Default.Payments
        }

        "MEMBER_JOINED" -> {
            Icons.Default.GroupAdd
        }

        else -> {
            Icons.Default.Groups
        }
    }
}

/*
 * Converts the Firestore timestamp into readable text.
 *
 * Example:
 * Jul 29, 9:25 PM
 */
private fun formatNotificationDate(
    timestamp: Long
): String {
    val formatter = SimpleDateFormat(
        "MMM d, h:mm a",
        Locale.getDefault()
    )

    return formatter.format(
        Date(timestamp)
    )
}