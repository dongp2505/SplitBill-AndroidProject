package week11.st560151.finalproject.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import week11.st560151.finalproject.ui.theme.BottomNavBackground
import week11.st560151.finalproject.ui.theme.BottomNavBorder

enum class BottomNavTab {
    Groups, Notifications, Profile
}

/** Bottom nav shown on the three top-level destinations (Groups, Notifications, Profile). */
@Composable
fun AppBottomNav(
    selected: BottomNavTab,
    onSelect: (BottomNavTab) -> Unit
) {
    Column {
        HorizontalDivider(thickness = 1.dp, color = BottomNavBorder)

        NavigationBar(
            containerColor = BottomNavBackground,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = selected == BottomNavTab.Groups,
                onClick = { onSelect(BottomNavTab.Groups) },
                icon = {
                    Icon(Icons.Filled.GridView, contentDescription = "Groups")
                }
            )

            NavigationBarItem(
                selected = selected == BottomNavTab.Notifications,
                onClick = { onSelect(BottomNavTab.Notifications) },
                icon = {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
            )

            NavigationBarItem(
                selected = selected == BottomNavTab.Profile,
                onClick = { onSelect(BottomNavTab.Profile) },
                icon = {
                    Icon(Icons.Filled.Person, contentDescription = "Profile")
                }
            )
        }
    }
}
