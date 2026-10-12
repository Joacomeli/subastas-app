package com.subastasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.subastasapp.data.api.RetrofitClient
import com.subastasapp.data.datastore.TokenManager
import com.subastasapp.data.repository.AuthRepository
import com.subastasapp.data.repository.UserRepository
import com.subastasapp.navigation.NavGraph
import com.subastasapp.ui.auth.*
import com.subastasapp.ui.profile.ProfileViewModel
import com.subastasapp.ui.theme.SubastasAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Inicialización simple de Repositorios y ViewModels
        val authApiService = RetrofitClient.getAuthApiService()
        val authRepository = AuthRepository(authApiService)
        val tokenManager = TokenManager(this)

        setContent {
            val userApiService = RetrofitClient.getUserApiService(this)
            val userRepository = UserRepository(userApiService)

            val loginViewModel: LoginViewModel = viewModel(
                factory = GenericViewModelFactory { LoginViewModel(authRepository, tokenManager) }
            )
            val registerViewModel: RegisterViewModel = viewModel(
                factory = GenericViewModelFactory { RegisterViewModel(authRepository, userRepository, tokenManager) }
            )
            val authViewModel: AuthViewModel = viewModel(
                factory = GenericViewModelFactory { AuthViewModel(authRepository) }
            )
            val profileViewModel: ProfileViewModel = viewModel(
                factory = GenericViewModelFactory { ProfileViewModel(userRepository, authRepository, tokenManager) }
            )

            SubastasAppTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    tokenManager = tokenManager,
                    loginViewModel = loginViewModel,
                    registerViewModel = registerViewModel,
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}

// Factory simple para inyectar dependencias en ViewModels sin Dagger/Hilt
class GenericViewModelFactory<T : androidx.lifecycle.ViewModel>(
    private val creator: () -> T
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
