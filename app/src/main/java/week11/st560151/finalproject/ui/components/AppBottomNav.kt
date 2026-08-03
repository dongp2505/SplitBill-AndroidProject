package week11.st560151.finalproject.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import week11.st560151.finalproject.ui.theme.BottomNavBackground
import week11.st560151.finalproject.ui.theme.BottomNavBorder

enum class BottomNavTab {
    Groups,
    Notifications,
    Profile
}

@Composable
fun AppBottomNav(
    selected: BottomNavTab,
    unreadNotificationCount: Int = 0,
    onSelect: (BottomNavTab) -> Unit
) {
    Column {
        HorizontalDivider(
            thickness = 1.dp,
            color = BottomNavBorder
        )

        NavigationBar(
            containerColor = BottomNavBackground,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = selected == BottomNavTab.Groups,
                onClick = { onSelect(BottomNavTab.Groups) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Groups"
                    )
                }
            )

            NavigationBarItem(
                selected = selected == BottomNavTab.Notifications,
                onClick = { onSelect(BottomNavTab.Notifications) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(
                                        text = if (unreadNotificationCount > 99) {
                                            "99+"
                                        } else {
                                            unreadNotificationCount.toString()
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = if (unreadNotificationCount > 0) {
                                "$unreadNotificationCount unread notifications"
                            } else {
                                "Notifications"
                            }
                        )
                    }
                }
            )

            NavigationBarItem(
                selected = selected == BottomNavTab.Profile,
                onClick = { onSelect(BottomNavTab.Profile) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile"
                    )
                }
            )
        }
    }
}