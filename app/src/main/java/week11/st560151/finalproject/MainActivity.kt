package week11.st560151.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import week11.st560151.finalproject.navigation.AppNavGraph
import week11.st560151.finalproject.ui.theme.SplitBillTheme
import week11.st560151.finalproject.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            SplitBillTheme {
                val navController =
                    rememberNavController()

                val authViewModel:
                        AuthViewModel = viewModel()

                AppNavGraph(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
    }
}