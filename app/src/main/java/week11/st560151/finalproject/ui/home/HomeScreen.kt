package week11.st560151.finalproject.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onGroupsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to SplitBill")

        Button(
            onClick = onGroupsClick,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(text = "View Groups")
        }

        Button(
            onClick = onLogoutClick,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(text = "Logout")
        }
    }
}