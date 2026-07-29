package week11.st560151.finalproject.navigation

sealed class Screen(
    val route: String
) {
    data object Login : Screen("login")

    data object Register : Screen("register")

    data object ForgotPassword :
        Screen("forgot_password")

    data object Home : Screen("home")

    data object Groups : Screen("groups")

    data object Settlement :
        Screen("settlement")
}