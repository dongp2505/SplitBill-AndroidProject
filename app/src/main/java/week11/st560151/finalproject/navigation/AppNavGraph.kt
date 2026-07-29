package week11.st560151.finalproject.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import week11.st560151.finalproject.ui.auth.ForgotPasswordScreen
import week11.st560151.finalproject.ui.auth.LoginScreen
import week11.st560151.finalproject.ui.auth.RegisterScreen
import week11.st560151.finalproject.ui.groups.GroupsScreen
import week11.st560151.finalproject.ui.home.HomeScreen
import week11.st560151.finalproject.ui.settlements.SettlementScreen
import week11.st560151.finalproject.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val startDestination =
        if (authViewModel.isSignedIn()) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        /*
         * LOGIN SCREEN
         */
        composable(
            route = Screen.Login.route
        ) {
            LoginScreen(
                authViewModel = authViewModel,

                onLoginSuccess = {
                    navController.navigate(
                        Screen.Home.route
                    ) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }

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

        /*
         * REGISTER SCREEN
         */
        composable(
            route = Screen.Register.route
        ) {
            RegisterScreen(
                authViewModel = authViewModel,

                onRegisterSuccess = {
                    navController.navigate(
                        Screen.Home.route
                    ) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                },

                onBackClick = {
                    authViewModel.resetRegisterState()
                    navController.popBackStack()
                }
            )
        }

        /*
         * FORGOT PASSWORD SCREEN
         */
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

        /*
         * HOME SCREEN
         */
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen(
                onGroupsClick = {
                    navController.navigate(
                        Screen.Groups.route
                    )
                },

                onSettleUpClick = {
                    navController.navigate(
                        Screen.Settlement.route
                    )
                },

                onLogoutClick = {
                    authViewModel.logout()

                    navController.navigate(
                        Screen.Login.route
                    ) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
        }

        /*
         * GROUPS SCREEN
         */
        composable(
            route = Screen.Groups.route
        ) {
            GroupsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        /*
         * SETTLEMENT SCREEN
         */
        composable(
            route = Screen.Settlement.route
        ) {
            val activity =
                LocalContext.current
                    .findFragmentActivity()

            if (activity != null) {
                SettlementScreen(
                    activity = activity,

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