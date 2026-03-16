package com.example.orderlistapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orderlistapp.viewmodel.OrderViewModel

data class UserSession(val username: String, val isAdmin: Boolean)

data class TabItem(val title: String, val icon: ImageVector, val badgeCount: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(session: UserSession = UserSession("Admin", true)) {
    val orderViewModel: OrderViewModel = viewModel()
    val message by orderViewModel.message.collectAsState()
    val activeOrders by orderViewModel.activeOrders.collectAsState()
    val pendingFull by orderViewModel.pendingFullOrders.collectAsState()
    val pendingMissing by orderViewModel.pendingMissingItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Instant offline boot - don't force network sync
    LaunchedEffect(Unit) { orderViewModel.loadAllData(force = false) }

    // Show snackbar messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!)
            orderViewModel.clearMessage()
        }
    }

    val tabs = listOf(
        TabItem("Active", Icons.Default.ShoppingCart, activeOrders.size),
        TabItem("Pending", Icons.Default.Warning, pendingFull.size + pendingMissing.size),
        TabItem("Dispatched", Icons.Default.CheckCircle),
        TabItem("Summary", Icons.Default.BarChart)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📦 Order Manager",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A237E))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab.badgeCount > 0) {
                                        Badge { Text(tab.badgeCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1A237E),
                            selectedTextColor = Color(0xFF1A237E),
                            indicatorColor = Color(0xFFE8EAF6)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            when (selectedTab) {
                0 -> ActiveOrdersScreen(viewModel = orderViewModel, isAdmin = session.isAdmin)
                1 -> PendingOrdersScreen(viewModel = orderViewModel)
                2 -> DispatchedOrdersScreen(viewModel = orderViewModel)
                3 -> SummaryScreen(viewModel = orderViewModel)
            }
        }
    }
}
