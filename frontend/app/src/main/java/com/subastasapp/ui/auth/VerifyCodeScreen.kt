package com.subastasapp.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    email: String,
    viewModel: AuthViewModel,
    onCodeVerified: (String) -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && (uiState as UiState.Success).data == "Código verificado") {
            onCodeVerified(code)
            viewModel.resetState()
        }
    }

    val maskedEmail = remember(email) {
        if (email.contains("@")) {
            val parts = email.split("@")
            val first = parts[0]
            val prefix = if (first.length >= 2) first.substring(0, 2) else first
            prefix + "****@" + parts[1]
        } else email
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Verificar código", fontSize = 18.sp) },
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
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Se ha enviado el código de verificación a $maskedEmail",
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.verifyCode(email, code) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Verificar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Reenviar código de verificación",
                        color = Color(0xFFC9A84C),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { viewModel.resendCode(email) }
                    )
                }
            }
            if (uiState is UiState.Error) {
                Text((uiState as UiState.Error).message, color = Color.Red, modifier = Modifier.padding(top = 16.dp))
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
