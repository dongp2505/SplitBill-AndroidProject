package week11.st560151.finalproject.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import week11.st560151.finalproject.ui.components.AppBottomNav
import week11.st560151.finalproject.ui.components.BottomNavTab
import week11.st560151.finalproject.ui.components.SecondaryButton

@Composable
fun ProfileScreen(
    onGroupsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val displayName = currentUser?.displayName?.ifBlank { null } ?: "SplitBill user"
    val email = currentUser?.email.orEmpty()
    val initial = (currentUser?.displayName?.firstOrNull()
        ?: currentUser?.email?.firstOrNull()
        ?: '?').uppercaseChar()

    Scaffold(
        bottomBar = {
            AppBottomNav(
                selected = BottomNavTab.Profile,
                onSelect = { tab ->
                    when (tab) {
                        BottomNavTab.Groups -> onGroupsClick()
                        BottomNavTab.Notifications -> onNotificationsClick()
                        BottomNavTab.Profile -> Unit
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
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = initial.toString(), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = displayName, fontWeight = FontWeight.Bold)

                    Text(
                        text = email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            SecondaryButton(
                text = "Sign out",
                onClick = onSignOutClick
            )
        }
    }
}
