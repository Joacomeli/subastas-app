package com.subastasapp.ui.main

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subastasapp.GenericViewModelFactory
import com.subastasapp.data.api.RetrofitClient
import com.subastasapp.data.realtime.PollingBidStream
import com.subastasapp.data.repository.RealAuctionRepository
import com.subastasapp.data.repository.UserRepository
import com.subastasapp.ui.auction.AuctionListScreen
import com.subastasapp.ui.auction.CatalogItemDetailScreen
import com.subastasapp.ui.auction.LiveAuctionScreen
import com.subastasapp.ui.consignar.*
import com.subastasapp.ui.profile.ProfileScreen
import com.subastasapp.ui.profile.ProfileViewModel
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    profileViewModel: ProfileViewModel,
    isGuest: Boolean = false
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            Column {
                if (isGuest) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0A1931)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Estás como invitado",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = {
                                rootNavController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }) {
                                Text(
                                    text = "Iniciá sesión",
                                    color = Color(0xFFC9A84C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Divider(color = Color(0xFFE9E9E9), thickness = 1.dp)
                BottomNavigationBar(navController = navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Home.route) {
                // Inicio: navegación anidada Subastas activas -> Detalle -> Subasta en vivo
                val homeNav = rememberNavController()
                // Repositorio real de subastas (REST) + stream de pujas por polling.
                val homeContext = LocalContext.current
                val subastaApi = remember { RetrofitClient.getSubastaApiService(homeContext) }
                val auctionRepo = remember { RealAuctionRepository(subastaApi) }
                val bidStream = remember { PollingBidStream(subastaApi) }
                NavHost(navController = homeNav, startDestination = "auctions_list") {
                    composable("auctions_list") {
                        AuctionListScreen(
                            isGuest = isGuest,
                            repository = auctionRepo,
                            onAuctionClick = { id -> homeNav.navigate("auction_detail/$id") },
                            onNotificationsClick = { homeNav.navigate("notificaciones") }
                        )
                    }
                    composable("auction_detail/{id}") { entry ->
                        val id = entry.arguments?.getString("id")?.toLongOrNull()
                        if (id != null) {
                            CatalogItemDetailScreen(
                                auctionId = id,
                                isGuest = isGuest,
                                repository = auctionRepo,
                                onVerEnVivo = { aId -> homeNav.navigate("auction_live/$aId") },
                                onBack = { homeNav.popBackStack() }
                            )
                        }
                    }
                    composable("auction_live/{id}") { entry ->
                        val id = entry.arguments?.getString("id")?.toLongOrNull()
                        if (id != null) {
                            LiveAuctionScreen(
                                auctionId = id,
                                isGuest = isGuest,
                                repository = auctionRepo,
                                bidStream = bidStream,
                                onBack = { homeNav.popBackStack() },
                                onRequireLogin = {
                                    rootNavController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                    composable("notificaciones") {
                        val context = LocalContext.current
                        val userApiService = remember { RetrofitClient.getUserApiService(context) }
                        val userRepository = remember { UserRepository(userApiService) }
                        val consignarViewModel: ConsignarViewModel = viewModel(
                            factory = GenericViewModelFactory { ConsignarViewModel(userRepository) }
                        )
                        NotificacionesScreen(
                            viewModel = consignarViewModel,
                            onBack = { homeNav.popBackStack() }
                        )
                    }
                }
            }
            composable(BottomNavItem.Consignar.route) {
                val consignarNav = rememberNavController()
                val context = LocalContext.current
                val userApiService = remember { RetrofitClient.getUserApiService(context) }
                val userRepository = remember { UserRepository(userApiService) }
                val consignarViewModel: ConsignarViewModel = viewModel(
                    factory = GenericViewModelFactory { ConsignarViewModel(userRepository) }
                )

                NavHost(navController = consignarNav, startDestination = "consignaciones_list") {
                    composable("consignaciones_list") {
                        MyConsignmentsScreen(
                            viewModel = consignarViewModel,
                            onNavigateToCreate = { consignarNav.navigate("crear_consignacion") },
                            onNavigateToDetail = { id -> consignarNav.navigate("consignacion_detail/$id") },
                            onNavigateToNotifications = { consignarNav.navigate("notificaciones") }
                        )
                    }
                    composable("crear_consignacion") {
                        CreateConsignmentScreen(
                            viewModel = consignarViewModel,
                            onBack = { consignarNav.popBackStack() }
                        )
                    }
                    composable("consignacion_detail/{id}") { entry ->
                        val id = entry.arguments?.getString("id")?.toLongOrNull()
                        if (id != null) {
                            ConsignmentDetailScreen(
                                consignmentId = id,
                                viewModel = consignarViewModel,
                                onBack = { consignarNav.popBackStack() }
                            )
                        }
                    }
                    composable("notificaciones") {
                        NotificacionesScreen(
                            viewModel = consignarViewModel,
                            onBack = { consignarNav.popBackStack() }
                        )
                    }
                }
            }
            composable(BottomNavItem.Perfil.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    isGuest = isGuest,
                    onNavigateToHistory = { /* TODO */ },
                    onNavigateToFavorites = { /* TODO */ },
                    onNavigateToConsignments = {
                        navController.navigate(BottomNavItem.Consignar.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLogout = {
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Consignar,
        BottomNavItem.Perfil
    )
    
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (item == BottomNavItem.Consignar) {
                        Surface(
                            shape = CircleShape,
                            border = ButtonDefaults.outlinedButtonBorder,
                            color = Color.Transparent,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        Icon(imageVector = item.icon, contentDescription = item.title)
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0A1931),
                    selectedTextColor = Color(0xFF0A1931),
                    unselectedIconColor = Color(0xFF555555),
                    unselectedTextColor = Color(0xFF555555),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
