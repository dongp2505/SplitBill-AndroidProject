package week11.st560151.finalproject.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import week11.st560151.finalproject.ui.auth.ForgotPasswordScreen
import week11.st560151.finalproject.ui.auth.LoginScreen
import week11.st560151.finalproject.ui.auth.RegisterScreen
import week11.st560151.finalproject.ui.expenses.AddExpenseScreen
import week11.st560151.finalproject.ui.groups.AddGroupScreen
import week11.st560151.finalproject.ui.groups.CreateGroupScreen
import week11.st560151.finalproject.ui.groups.GroupDetailScreen
import week11.st560151.finalproject.ui.groups.GroupReadyScreen
import week11.st560151.finalproject.ui.groups.GroupsScreen
import week11.st560151.finalproject.ui.groups.JoinGroupScreen
import week11.st560151.finalproject.ui.notifications.NotificationsScreen
import week11.st560151.finalproject.ui.profile.ProfileScreen
import week11.st560151.finalproject.ui.settlements.SettlementScreen
import week11.st560151.finalproject.ui.start.StartScreen
import week11.st560151.finalproject.viewmodel.AuthViewModel
import week11.st560151.finalproject.viewmodel.SettlementViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val startDestination =
        if (authViewModel.isSignedIn()) {
            Screen.Groups.route
        } else {
            Screen.Start.route
        }

    // Preserves each tab's own back-stack/scroll state across switches.
    fun switchTab(
        route: String
    ) {
        navController.navigate(route) {
            popUpTo(Screen.Groups.route) {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(
            route = Screen.Start.route
        ) {
            StartScreen(
                onGetStartedClick = {
                    navController.navigate(
                        Screen.Register.route
                    )
                },

                onSignInClick = {
                    navController.navigate(
                        Screen.Login.route
                    )
                }
            )
        }

        composable(
            route = Screen.Login.route
        ) {
            LoginScreen(
                authViewModel = authViewModel,

                onLoginSuccess = {
                    navController.navigate(
                        Screen.Groups.route
                    ) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                },

                onRegisterClick = {
                    navController.navigate(
                        Screen.Register.route
                    )
                },

                onForgotPasswordClick = {
                    navController.navigate(
                        Screen.ForgotPassword.route
                    )
                }
            )
        }

        composable(
            route = Screen.Register.route
        ) {
            RegisterScreen(
                authViewModel = authViewModel,

                onRegisterSuccess = {
                    navController.navigate(
                        Screen.Groups.route
                    ) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                },

                onBackClick = {
                    authViewModel.resetRegisterState()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ForgotPassword.route
        ) {
            ForgotPasswordScreen(
                authViewModel = authViewModel,

                onBackClick = {
                    authViewModel.resetPasswordState()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Groups.route
        ) {
            GroupsScreen(
                onGroupClick = { groupId ->
                    navController.navigate(
                        Screen.GroupDetail.createRoute(
                            groupId
                        )
                    )
                },

                onAddGroupClick = {
                    navController.navigate(
                        Screen.AddGroup.route
                    )
                },

                onNotificationsClick = {
                    switchTab(
                        Screen.Notifications.route
                    )
                },

                onProfileClick = {
                    switchTab(
                        Screen.Profile.route
                    )
                }
            )
        }

        composable(
            route = Screen.Notifications.route
        ) {
            NotificationsScreen(
                onGroupsClick = {
                    switchTab(
                        Screen.Groups.route
                    )
                },

                onProfileClick = {
                    switchTab(
                        Screen.Profile.route
                    )
                },

                onNotificationClick = { groupId ->
                    navController.navigate(
                        Screen.GroupDetail.createRoute(
                            groupId
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.Profile.route
        ) {
            ProfileScreen(
                onGroupsClick = {
                    switchTab(
                        Screen.Groups.route
                    )
                },

                onNotificationsClick = {
                    switchTab(
                        Screen.Notifications.route
                    )
                },

                onSignOutClick = {

                    navController.navigate(
                        Screen.Start.route
                    ) {
                        popUpTo(
                            navController.graph.startDestinationId
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                        restoreState = false
                    }


                    authViewModel.logout()
                }
            )
        }

        composable(
            route = Screen.AddGroup.route
        ) {
            AddGroupScreen(
                onBackClick = {
                    navController.popBackStack()
                },

                onCreateGroupClick = {
                    navController.navigate(
                        Screen.CreateGroup.route
                    )
                },

                onJoinGroupClick = {
                    navController.navigate(
                        Screen.JoinGroup.route
                    )
                }
            )
        }

        composable(
            route = Screen.CreateGroup.route
        ) {
            CreateGroupScreen(
                onBackClick = {
                    navController.popBackStack()
                },

                onGroupCreated = { groupId ->
                    navController.navigate(
                        Screen.GroupReady.createRoute(
                            groupId
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.JoinGroup.route
        ) {
            JoinGroupScreen(
                onBackClick = {
                    navController.popBackStack()
                },

                onGroupJoined = {
                    navController.navigate(
                        Screen.Groups.route
                    ) {
                        popUpTo(
                            Screen.Groups.route
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.GroupReady.route,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val groupId = backStackEntry
                .arguments
                ?.getString("groupId")
                .orEmpty()

            GroupReadyScreen(
                groupId = groupId,

                onContinueClick = {
                    navController.navigate(
                        Screen.Groups.route
                    ) {
                        popUpTo(
                            Screen.Groups.route
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val groupId = backStackEntry
                .arguments
                ?.getString("groupId")
                .orEmpty()

            GroupDetailScreen(
                groupId = groupId,

                onBackClick = {
                    navController.popBackStack()
                },

                onAddExpenseClick = {
                    navController.navigate(
                        Screen.AddExpense.createRoute(
                            groupId
                        )
                    )
                },

                onSettleUpClick = {
                        settleGroupId,
                        payerEmail,
                        receiverEmail,
                        amount ->

                    navController.navigate(
                        Screen.Settlement.createRoute(
                            groupId = settleGroupId,
                            payerEmail = payerEmail,
                            receiverEmail = receiverEmail,
                            amount = amount
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val groupId = backStackEntry
                .arguments
                ?.getString("groupId")
                .orEmpty()

            AddExpenseScreen(
                groupId = groupId,

                onBackClick = {
                    navController.popBackStack()
                },

                onExpenseSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Settlement.route,
            arguments = listOf(
                navArgument("groupId") {
                    defaultValue = ""
                },

                navArgument("payerEmail") {
                    defaultValue = ""
                },

                navArgument("receiverEmail") {
                    defaultValue = ""
                },

                navArgument("amount") {
                    defaultValue = "0.0"
                }
            )
        ) { backStackEntry ->

            val activity = LocalContext
                .current
                .findFragmentActivity()

            if (activity != null) {
                val settlementViewModel:
                        SettlementViewModel = viewModel()

                val groupId = backStackEntry
                    .arguments
                    ?.getString("groupId")
                    .orEmpty()

                val payerEmail = backStackEntry
                    .arguments
                    ?.getString("payerEmail")
                    .orEmpty()

                val receiverEmail = backStackEntry
                    .arguments
                    ?.getString("receiverEmail")
                    .orEmpty()

                val amount = backStackEntry
                    .arguments
                    ?.getString("amount")
                    ?.toDoubleOrNull()
                    ?: 0.0

                LaunchedEffect(
                    groupId,
                    payerEmail,
                    receiverEmail,
                    amount
                ) {
                    if (
                        payerEmail.isNotBlank() ||
                        receiverEmail.isNotBlank()
                    ) {
                        settlementViewModel.prefill(
                            groupId = groupId,
                            payerEmail = payerEmail,
                            receiverEmail = receiverEmail,
                            amount = amount
                        )
                    }
                }

                SettlementScreen(
                    activity = activity,
                    settlementViewModel =
                        settlementViewModel,

                    onBackClick = {
                        navController.popBackStack()
                    },

                    onSettlementCompleted = {
                        navController.popBackStack()
                    }
                )
            } else {
                Text(
                    text = "Unable to start biometric authentication."
                )
            }
        }
    }
}

// Biometric prompts need a FragmentActivity, not just any Context.
private tailrec fun Context.findFragmentActivity():
        FragmentActivity? {

    return when (this) {
        is FragmentActivity -> {
            this
        }

        is ContextWrapper -> {
            baseContext.findFragmentActivity()
        }

        else -> {
            null
        }
    }
}