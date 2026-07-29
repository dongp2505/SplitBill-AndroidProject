package week11.st560151.finalproject.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import week11.st560151.finalproject.data.model.Settlement
import week11.st560151.finalproject.viewmodel.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onGroupsClick: () -> Unit,
    onSettleUpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val state by
    homeViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SplitBill",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onLogoutClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement =
                    Arrangement.Center,
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }

            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = if (
                        state.displayName.isBlank()
                    ) {
                        "Welcome Back"
                    } else {
                        "Welcome, ${state.displayName}"
                    },
                    style =
                        MaterialTheme
                            .typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Manage your shared expenses and settlements.",
                    color =
                        MaterialTheme
                            .colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    BalanceCard(
                        title = "You Paid",
                        amount =
                            formatMoney(
                                state.totalOwed
                            ),
                        modifier =
                            Modifier.weight(1f)
                    )

                    BalanceCard(
                        title = "You Received",
                        amount =
                            formatMoney(
                                state.totalOwedToUser
                            ),
                        modifier =
                            Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Quick Actions",
                    style =
                        MaterialTheme
                            .typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Button(
                    onClick = onGroupsClick,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Groups,
                        contentDescription = null
                    )

                    Text(
                        text = "View Groups",
                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            )
                    )
                }
            }

            item {
                Button(
                    onClick = onSettleUpClick,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Payments,
                        contentDescription = null
                    )

                    Text(
                        text = "Settle Up",
                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            )
                    )
                }
            }

            item {
                Text(
                    text = "Recent Activity",
                    style =
                        MaterialTheme
                            .typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (
                state.recentSettlements.isEmpty()
            ) {
                item {
                    Text(
                        text =
                            "No settlement activity yet.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            } else {
                items(
                    items =
                        state.recentSettlements,
                    key = {
                        it.id
                    }
                ) { settlement ->
                    SettlementActivityCard(
                        settlement = settlement
                    )
                }
            }

            state.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color =
                            MaterialTheme
                                .colorScheme.error
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme
                    .colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(title)

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = amount,
                style =
                    MaterialTheme
                        .typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettlementActivityCard(
    settlement: Settlement
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text =
                    "${settlement.payerName} paid ${settlement.receiverName}",
                fontWeight = FontWeight.Medium
            )

            Text(
                text = formatMoney(
                    settlement.amount
                ),
                color =
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatMoney(
    amount: Double
): String {
    return String.format(
        Locale.US,
        "$%.2f",
        amount
    )
}