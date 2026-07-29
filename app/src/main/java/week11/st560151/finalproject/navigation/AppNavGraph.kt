package week11.st560151.finalproject.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import week11.st560151.finalproject.ui.auth.ForgotPasswordScreen
import week11.st560151.finalproject.ui.auth.LoginScreen
import week11.st560151.finalproject.ui.auth.RegisterScreen
import week11.st560151.finalproject.ui.groups.GroupsScreen
import week11.st560151.finalproject.ui.home.HomeScreen
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
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
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

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onGroupsClick = {
                    navController.navigate(
                        Screen.Groups.route
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

        composable(Screen.Groups.route) {
            GroupsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}