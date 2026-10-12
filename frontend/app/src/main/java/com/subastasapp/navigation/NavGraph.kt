package com.subastasapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.subastasapp.data.datastore.TokenManager
import com.subastasapp.ui.auth.*
import com.subastasapp.ui.main.MainScreen
import com.subastasapp.ui.profile.ProfileViewModel
import com.subastasapp.ui.splash.SplashScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object RegisterStep1 : Screen("register_step1")
    object RegisterStep2 : Screen("register_step2")
    object RegisterStep3 : Screen("register_step3")
    object ForgotPassword : Screen("forgot_password")
    object VerifyCode : Screen("verify_code/{email}") {
        fun createRoute(email: String) = "verify_code/$email"
    }
    object ChangePassword : Screen("change_password/{email}/{code}") {
        fun createRoute(email: String, code: String) = "change_password/$email/$code"
    }
    object Main : Screen("main")

    object Guest : Screen("guest")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    tokenManager: TokenManager,
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            val scope = rememberCoroutineScope()
            SplashScreen(onFinished = {
                // Lógica de decisión de destino inicial basada en JWT
                scope.launch {
                    val token = tokenManager.token.first()
                    if (!token.isNullOrBlank()) {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            })
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.RegisterStep1.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onEnterAsGuest = {
                    navController.navigate(Screen.Guest.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                },
                // DEBUG: entra al flujo de usuario registrado sin backend.
                onEnterAsDemo = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.RegisterStep1.route) {
            RegisterStep1Screen(
                viewModel = registerViewModel,
                onNext = { navController.navigate(Screen.RegisterStep2.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RegisterStep2.route) {
            RegisterStep2Screen(
                viewModel = registerViewModel,
                onNext = { navController.navigate(Screen.RegisterStep3.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.RegisterStep3.route) {
            RegisterStep3Screen(
                viewModel = registerViewModel,
                onFinish = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onCodeSent = { email ->
                    navController.navigate(Screen.VerifyCode.createRoute(email))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VerifyCode.route) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerifyCodeScreen(
                email = email,
                viewModel = authViewModel,
                onCodeVerified = { code ->
                    navController.navigate(Screen.ChangePassword.createRoute(email, code))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChangePassword.route) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val code = backStackEntry.arguments?.getString("code") ?: ""
            ChangePasswordScreen(
                email = email,
                code = code,
                viewModel = authViewModel,
                onPasswordChanged = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                rootNavController = navController,
                profileViewModel = profileViewModel
            )
        }

        composable(Screen.Guest.route) {
            MainScreen(
                rootNavController = navController,
                profileViewModel = profileViewModel,
                isGuest = true
            )
        }
    }
}
