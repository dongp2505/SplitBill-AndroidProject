package week11.st560151.finalproject.ui.notifications

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import week11.st560151.finalproject.data.model.AppNotification
import week11.st560151.finalproject.ui.components.AppBottomNav
import week11.st560151.finalproject.ui.components.BottomNavTab
import week11.st560151.finalproject.ui.theme.CardBackground
import week11.st560151.finalproject.ui.theme.CardBorder
import week11.st560151.finalproject.ui.theme.BrandGreenLight
import week11.st560151.finalproject.ui.theme.LinkGreen
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

    val unreadNotificationCount = notifications.count { notification ->
        !notification.read
    }

    Scaffold(
        bottomBar = {
            AppBottomNav(
                selected = BottomNavTab.Notifications,
                unreadNotificationCount = unreadNotificationCount,
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 22.dp,
                        bottom = 14.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (unreadNotificationCount > 0) {
                    Text(
                        text = "Read all",
                        color = LinkGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            notificationViewModel.markAllAsRead()
                        }
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 10.dp
                    )
                )
            }

            Crossfade(targetState = notifications.isEmpty(), label = "notificationsEmpty") { isEmpty ->
                if (isEmpty) {
                    EmptyNotificationsContent(
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp
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
                                    if (notification.read) {
                                        if (notification.groupId.isNotBlank()) {
                                            onNotificationClick(notification.groupId)
                                        }
                                    } else {
                                        notificationViewModel.markAsRead(
                                            notificationId = notification.id,
                                            onSuccess = {
                                                if (notification.groupId.isNotBlank()) {
                                                    onNotificationClick(notification.groupId)
                                                }
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
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
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme
                        .onSecondaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = "No notifications yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "Group and settlement updates will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme
                    .onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor =
        if (notification.read) {
            CardBackground
        } else {
            BrandGreenLight
        }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme
                            .secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notificationIcon(
                        notification.type
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme
                        .onSecondaryContainer,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography
                            .titleSmall,
                        fontWeight =
                            if (notification.read) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Bold
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = formatNotificationTime(
                            notification.createdAt
                        ),
                        style = MaterialTheme.typography
                            .labelSmall,
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant
                    )
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.read) {
                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                val unreadDotTransition = rememberInfiniteTransition(label = "unreadDot")

                val unreadDotScale by unreadDotTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 700),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "unreadDotScale"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(unreadDotScale)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

private fun notificationIcon(
    notificationType: String
): ImageVector {
    return when (notificationType) {
        "SETTLEMENT_COMPLETED" -> {
            Icons.Default.Payments
        }

        "MEMBER_JOINED",
        "MEMBER_ADDED",
        "GROUP_CREATED" -> {
            Icons.Default.GroupAdd
        }

        "EXPENSE_ADDED" -> {
            Icons.Default.ReceiptLong
        }

        else -> {
            Icons.Default.Groups
        }
    }
}

private fun formatNotificationTime(
    timestamp: Long
): String {
    if (timestamp <= 0L) {
        return ""
    }

    val now =
        System.currentTimeMillis()

    val difference =
        now - timestamp

    val minute =
        60_000L

    val hour =
        60 * minute

    val day =
        24 * hour

    return when {
        difference < minute -> {
            "Now"
        }

        difference < hour -> {
            "${difference / minute}m"
        }

        difference < day -> {
            "${difference / hour}h"
        }

        difference < 7 * day -> {
            "${difference / day}d"
        }

        else -> {
            SimpleDateFormat(
                "MMM d",
                Locale.getDefault()
            ).format(
                Date(timestamp)
            )
        }
    }
}