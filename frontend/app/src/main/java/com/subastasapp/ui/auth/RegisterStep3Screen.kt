package com.subastasapp.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.subastasapp.data.model.request.PaymentMethodRequest
import androidx.compose.material.icons.filled.Edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep3Screen(
    viewModel: RegisterViewModel,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods()
    }

    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && (uiState as UiState.Success).data == "Registro finalizado") {
            android.widget.Toast.makeText(context, "¡Registro completado con éxito!", android.widget.Toast.LENGTH_LONG).show()
            onFinish()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Registro - Paso 3 de 3", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                    }
                )
                Divider(color = Color(0xFFC9A84C), thickness = 2.dp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Agrega al menos un método de pago", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Para poder participar en las subastas de alta gama, necesitamos verificar tu saldo",
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )
                
                if (uiState is UiState.Error) {
                    Text((uiState as UiState.Error).message, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                PaymentOptionCard("Cuenta Bancaria", "CBU/CVU para transferencia", Icons.Default.AccountBalance) { showAddDialog = "BANK" }
                Spacer(modifier = Modifier.height(12.dp))
                PaymentOptionCard("Tarjeta de crédito", "Visa, Mastercard o American Express", Icons.Default.CreditCard) { showAddDialog = "CARD" }
                Spacer(modifier = Modifier.height(12.dp))
                PaymentOptionCard("Cheques certificados", "Presentación física de avales bancarios", Icons.Default.ReceiptLong) { showAddDialog = "CHEQUE" }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Medios verificados", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(paymentMethods) { method ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(method.tipo, fontWeight = FontWeight.Bold)
                            Text(method.descripcion, fontSize = 12.sp, color = Color.Gray)
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text("VERIFICADO", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.Green)
                        )
                        // Acciones por método: editar + borrar. Dos IconButton hermanos en
                        // un Row (no anidados) para que cada uno capture su propio click.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.editPaymentMethod(method.id) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = Color.Blue
                                )
                            }
                            IconButton(onClick = { viewModel.deletePaymentMethod(method.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { viewModel.finishRegistrationStep3() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A84C)),
                    shape = RoundedCornerShape(50.dp),
                    enabled = paymentMethods.isNotEmpty() && uiState !is UiState.Loading
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Terminar registro", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showAddDialog != null) {
            AddPaymentDialog(
                type = showAddDialog!!,
                onDismiss = { showAddDialog = null },
                onAdd = { request ->
                    viewModel.addPaymentMethod(request)
                    showAddDialog = null
                }
            )
        }
    }
}

@Composable
fun PaymentOptionCard(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0A1931), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(
                onClick = onClick,
                modifier = Modifier.background(Color(0xFF0A1931), CircleShape).size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(type: String, onDismiss: () -> Unit, onAdd: (PaymentMethodRequest) -> Unit) {
    var f1 by remember { mutableStateOf("") }
    var f2 by remember { mutableStateOf("") }
    var f3 by remember { mutableStateOf("") }
    var f4 by remember { mutableStateOf("") }
    var f5 by remember { mutableStateOf("") }
    var f6 by remember { mutableStateOf("") }
    var f7 by remember { mutableStateOf("") }
    var f8 by remember { mutableStateOf("") }
    var f9 by remember { mutableStateOf("") }

    var selectedTipoCuenta by remember { mutableStateOf("Caja de ahorro") }
    var selectedMoneda by remember { mutableStateOf("ARS") }

    var frenteUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var dorsoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val launcherFrente = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { frenteUri = it }
    val launcherDorso = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { dorsoUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(when(type) {
                "CARD" -> "Agregar tarjeta"
                "BANK" -> "Agregar cuenta bancaria"
                else -> "Agregar cheque certificado"
            })
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                when(type) {
                    "BANK" -> {
                        OutlinedTextField(value = f3, onValueChange = { f3 = it }, label = { Text("Titular de la cuenta") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f4, onValueChange = { f4 = it }, label = { Text("Documento del titular") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f1, onValueChange = { f1 = it }, label = { Text("Banco") }, modifier = Modifier.fillMaxWidth())
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tipo de cuenta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedTipoCuenta == "Caja de ahorro", onClick = { selectedTipoCuenta = "Caja de ahorro" })
                            Text("Caja de ahorro", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(selected = selectedTipoCuenta == "Cuenta corriente", onClick = { selectedTipoCuenta = "Cuenta corriente" })
                            Text("Cuenta corriente", fontSize = 12.sp)
                        }

                        OutlinedTextField(value = f6, onValueChange = { f6 = it }, label = { Text("Número de cuenta") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f2, onValueChange = { f2 = it }, label = { Text("CBU") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f7, onValueChange = { f7 = it }, label = { Text("Alias") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f5, onValueChange = { f5 = it }, label = { Text("CUIT/CUIL") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Moneda de la cuenta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedMoneda == "ARS", onClick = { selectedMoneda = "ARS" })
                            Text("ARS", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = selectedMoneda == "USD", onClick = { selectedMoneda = "USD" })
                            Text("USD", fontSize = 12.sp)
                        }
                    }
                    "CARD" -> {
                        OutlinedTextField(value = f1, onValueChange = { f1 = it }, label = { Text("Número de tarjeta") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f2, onValueChange = { f2 = it }, label = { Text("Nombre del titular") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f3, onValueChange = { f3 = it }, label = { Text("Vencimiento (MM/AA)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f4, onValueChange = { f4 = it }, label = { Text("Código de seguridad") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f5, onValueChange = { f5 = it }, label = { Text("Documento del titular") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f6, onValueChange = { f6 = it }, label = { Text("Banco") }, modifier = Modifier.fillMaxWidth())
                    }
                    "CHEQUE" -> {
                        OutlinedTextField(value = f1, onValueChange = { f1 = it }, label = { Text("Banco emisor") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f2, onValueChange = { f2 = it }, label = { Text("Número de cheque") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f3, onValueChange = { f3 = it }, label = { Text("Titular del cheque") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f4, onValueChange = { f4 = it }, label = { Text("Documento / CUIT del titular") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f7, onValueChange = { f7 = it }, label = { Text("Fecha de emisión (DD/MM/AAAA)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f5, onValueChange = { f5 = it }, label = { Text("Fecha de vencimiento (DD/MM/AAAA)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = f8, onValueChange = { f8 = it }, label = { Text("Monto autorizado") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f6, onValueChange = { f6 = it }, label = { Text("Número de cuenta asociada") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = f9, onValueChange = { f9 = it }, label = { Text("Sucursal bancaria") }, modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fotos del cheque", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PhotoBox(uri = frenteUri, label = "Frente", onClick = { launcherFrente.launch("image/*") }, modifier = Modifier.weight(1f))
                            PhotoBox(uri = dorsoUri, label = "Dorso", onClick = { launcherDorso.launch("image/*") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val request = when(type) {
                    "BANK" -> PaymentMethodRequest(
                        type = "BANK_ACCOUNT",
                        titular = f3,
                        documentoTitular = f4,
                        banco = f1,
                        tipoCuenta = if (selectedTipoCuenta == "Caja de ahorro") "CAJA_AHORRO" else "CUENTA_CORRIENTE",
                        numeroCuenta = f6,
                        cbu = f2,
                        alias = f7,
                        cuitCuil = f5,
                        moneda = selectedMoneda
                    )
                    "CARD" -> PaymentMethodRequest(
                        type = "CREDIT_CARD",
                        numero = f1,
                        titular = f2,
                        vencimiento = f3,
                        cvv = f4,
                        documentoTitular = f5,
                        banco = f6,
                        numeroUltimos4 = if (f1.length >= 4) f1.takeLast(4) else f1
                    )
                    else -> PaymentMethodRequest(
                        type = "CHEQUE",
                        banco = f1,
                        numeroCheque = f2,
                        titular = f3,
                        documentoTitular = f4,
                        fechaEmision = f7,
                        vencimiento = f5,
                        monto = f8.toDoubleOrNull(),
                        numeroCuenta = f6,
                        sucursal = f9,
                        fotoFrentePath = frenteUri?.toString(),
                        fotoDorsoPath = dorsoUri?.toString()
                    )
                }
                onAdd(request)
            }) { Text("Agregar") }
        }
    )
}

@Composable
fun PhotoBox(uri: android.net.Uri?, label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}
