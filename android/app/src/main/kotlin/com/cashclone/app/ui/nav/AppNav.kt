package com.cashclone.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cashclone.app.data.repository.CashCloneRepository
import com.cashclone.app.ui.screens.auth.LoginScreen
import com.cashclone.app.ui.screens.auth.SignupScreen
import com.cashclone.app.ui.screens.auth.WelcomeScreen
import com.cashclone.app.ui.screens.history.HistoryScreen
import com.cashclone.app.ui.screens.home.HomeScreen
import com.cashclone.app.ui.screens.kyc.KycScreen
import com.cashclone.app.ui.screens.profile.ProfileScreen
import com.cashclone.app.ui.screens.request.RequestScreen
import com.cashclone.app.ui.screens.send.SendScreen
import com.cashclone.app.viewmodel.AuthViewModel
import com.cashclone.app.viewmodel.GenericViewModelFactory
import com.cashclone.app.viewmodel.HistoryViewModel
import com.cashclone.app.viewmodel.HomeViewModel
import com.cashclone.app.viewmodel.KycViewModel
import com.cashclone.app.viewmodel.SendRequestViewModel

private object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val KYC = "kyc"
    const val HOME = "home"
    const val SEND = "send"
    const val REQUEST = "request"
    const val HISTORY = "history"
    const val PROFILE = "profile"
}

@Composable
fun AppNav(repository: CashCloneRepository) {
    val navController = rememberNavController()
    val factory = GenericViewModelFactory(
        repository,
        mapOf(
            AuthViewModel::class.java to { repo -> AuthViewModel(repo) },
            KycViewModel::class.java to { repo -> KycViewModel(repo) },
            HomeViewModel::class.java to { repo -> HomeViewModel(repo) },
            SendRequestViewModel::class.java to { repo -> SendRequestViewModel(repo) },
            HistoryViewModel::class.java to { repo -> HistoryViewModel(repo) },
        ),
    )
    val authViewModel: AuthViewModel = viewModel(factory = factory)
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    NavHost(navController = navController, startDestination = Routes.WELCOME) {
        composable(Routes.WELCOME) {
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) navController.navigateToHome()
            }
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.LOGIN) },
                onSignup = { navController.navigate(Routes.SIGNUP) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(authViewModel) { navController.navigateToHome() }
        }
        composable(Routes.SIGNUP) {
            SignupScreen(authViewModel) { navController.navigateToHome() }
        }
        composable(Routes.KYC) {
            val kycViewModel: KycViewModel = viewModel(factory = factory)
            KycScreen(kycViewModel) { navController.popBackStack() }
        }
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = homeViewModel,
                onSend = { navController.navigate(Routes.SEND) },
                onRequest = { navController.navigate(Routes.REQUEST) },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onProfile = { navController.navigate(Routes.PROFILE) },
                onNeedsKyc = { navController.navigate(Routes.KYC) },
            )
        }
        composable(Routes.SEND) {
            val sendViewModel: SendRequestViewModel = viewModel(factory = factory)
            SendScreen(sendViewModel) { navController.popBackStack() }
        }
        composable(Routes.REQUEST) {
            val requestViewModel: SendRequestViewModel = viewModel(factory = factory)
            RequestScreen(requestViewModel) { navController.popBackStack() }
        }
        composable(Routes.HISTORY) {
            val historyViewModel: HistoryViewModel = viewModel(factory = factory)
            HistoryScreen(historyViewModel)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(repository, authViewModel) {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(0)
                }
            }
        }
    }
}

private fun NavHostController.navigateToHome() {
    navigate("home") { popUpTo(0) }
}
