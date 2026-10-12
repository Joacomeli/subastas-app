package com.subastasapp.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home_tab", "INICIO", Icons.Default.Home)
    object Consignar : BottomNavItem("consignar_tab", "CONSIGNAR", Icons.Default.Add)
    object Perfil : BottomNavItem("perfil_tab", "PERFIL", Icons.Default.Person)
}
