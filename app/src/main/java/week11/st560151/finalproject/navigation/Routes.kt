package week11.st560151.finalproject.navigation

import java.net.URLEncoder

sealed class Screen(
    val route: String
) {
    data object Start :
        Screen("start")

    data object Login :
        Screen("login")

    data object Register :
        Screen("register")

    data object ForgotPassword :
        Screen("forgot_password")

    // Groups is the post-login landing page (see GroupsScreen).
    data object Groups :
        Screen("groups")

    data object Notifications :
        Screen("notifications")

    data object Profile :
        Screen("profile")

    data object AddGroup :
        Screen("add_group")

    data object CreateGroup :
        Screen("create_group")

    data object JoinGroup :
        Screen("join_group")

    data object GroupReady :
        Screen("group_ready/{groupId}") {
        fun createRoute(groupId: String) = "group_ready/$groupId"
    }

    data object GroupDetail :
        Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }

    data object AddExpense :
        Screen("add_expense/{groupId}") {
        fun createRoute(groupId: String) = "add_expense/$groupId"
    }

    data object Settlement :
        Screen(
            "settlement?groupId={groupId}&payerEmail={payerEmail}" +
                "&receiverEmail={receiverEmail}&amount={amount}"
        ) {
        fun createRoute(
            groupId: String = "",
            payerEmail: String = "",
            receiverEmail: String = "",
            amount: Double = 0.0
        ): String {
            fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

            return "settlement?groupId=${encode(groupId)}" +
                "&payerEmail=${encode(payerEmail)}" +
                "&receiverEmail=${encode(receiverEmail)}" +
                "&amount=$amount"
        }
    }
}
