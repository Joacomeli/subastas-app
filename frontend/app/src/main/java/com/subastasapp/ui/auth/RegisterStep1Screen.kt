package com.subastasapp.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep1Screen(
    viewModel: RegisterViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var domicilio by remember { mutableStateOf("") }
    var pais by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var frenteUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var dorsoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && (uiState as UiState.Success).data == "Paso 1 completado") {
            android.widget.Toast.makeText(context, "Datos personales validados correctamente", android.widget.Toast.LENGTH_SHORT).show()
            onNext()
            viewModel.resetState()
        }
    }

    val launcherFrente = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { frenteUri = it }
    val launcherDorso = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { dorsoUri = it }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Registro - Paso 1 de 3", fontSize = 18.sp) },
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Datos personales", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(value = nombre, onValueChange = { nombre = it; localError = null }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = apellido, onValueChange = { apellido = it; localError = null }, label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = domicilio, onValueChange = { domicilio = it; localError = null }, label = { Text("Domicilio legal") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = pais, onValueChange = { pais = it; localError = null }, label = { Text("País de origen") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; localError = null },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text("Documentación de identidad", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PhotoPicker(
                    uri = frenteUri,
                    label = "Foto DNI Frente",
                    onClick = { launcherFrente.launch("image/*"); localError = null },
                    modifier = Modifier.weight(1f)
                )
                PhotoPicker(
                    uri = dorsoUri,
                    label = "Foto DNI Dorso",
                    onClick = { launcherDorso.launch("image/*"); localError = null },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            val errorToShow = localError ?: (if (uiState is UiState.Error) (uiState as UiState.Error).message else null)
            if (errorToShow != null) {
                Text(errorToShow, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp), textAlign = TextAlign.Center)
            }

            Button(
                onClick = {
                    val tNombre = nombre.trim()
                    val tApellido = apellido.trim()
                    
                    val tEmail = email.trim()
                    if (tNombre.isBlank() || tApellido.isBlank()) {
                        localError = "El nombre y apellido son obligatorios"
                    } else if (tEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(tEmail).matches()) {
                        localError = "Ingresá un correo electrónico válido"
                    } else if (frenteUri == null || dorsoUri == null) {
                        localError = "Debes cargar ambas fotos del DNI"
                    } else {
                        localError = null
                        val frenteFile = uriToFile(context, frenteUri!!, "frente.jpg")
                        val dorsoFile = uriToFile(context, dorsoUri!!, "dorso.jpg")
                        // El email se valida y se guarda acá; en el Paso 2 sólo se setea la clave.
                        viewModel.tempEmail = tEmail
                        viewModel.registerStep1(tNombre, tApellido, domicilio.trim(), pais.trim(), frenteFile, dorsoFile)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                shape = RoundedCornerShape(50.dp)
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Continuar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PhotoPicker(uri: android.net.Uri?, label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

fun uriToFile(context: android.content.Context, uri: android.net.Uri, fileName: String): File {
    val file = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}
