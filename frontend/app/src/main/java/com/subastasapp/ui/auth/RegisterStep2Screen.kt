package com.subastasapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep2Screen(
    viewModel: RegisterViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    // El email se cargó y validó en el Paso 1; acá sólo creamos la clave.
    val emailFromStep1 = viewModel.tempEmail
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && (uiState as UiState.Success).data == "Paso 2 completado") {
            onNext()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Registro - Paso 2 de 3", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                    }
                )
                Divider(color = Color(0xFFC9A84C), thickness = 2.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9E9E9)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF0A1931))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Hemos verificado tu identidad. Ahora creá tu contraseña para asegurar tu cuenta de pujas.",
                            fontSize = 14.sp,
                            color = Color(0xFF555555)
                        )
                    }
                    if (emailFromStep1.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Correo: $emailFromStep1",
                            fontSize = 12.sp,
                            color = Color(0xFF0A1931),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    localError = null
                },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    localError = null
                },
                label = { Text("Confirmar Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            val errorToShow = localError ?: (if (uiState is UiState.Error) (uiState as UiState.Error).message else null)
            if (errorToShow != null) {
                Text(errorToShow, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp), textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    val trimmedPassword = password.trim()
                    val trimmedConfirm = confirmPassword.trim()

                    when {
                        // Defensa por si alguien llegó al paso 2 sin pasar por el paso 1.
                        emailFromStep1.isBlank() ->
                            localError = "Falta el correo electrónico. Volvé al Paso 1."
                        trimmedPassword.length < 8 ->
                            localError = "La contraseña debe tener al menos 8 caracteres"
                        trimmedPassword != trimmedConfirm ->
                            localError = "Las contraseñas no coinciden"
                        else -> {
                            localError = null
                            viewModel.completeRegistration(emailFromStep1, trimmedPassword)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = uiState !is UiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                shape = RoundedCornerShape(50.dp)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Continuar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Tu seguridad es nuestra prioridad. Utilizamos cifrado de grado bancario para proteger tus datos y transacciones.",
                fontSize = 12.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
