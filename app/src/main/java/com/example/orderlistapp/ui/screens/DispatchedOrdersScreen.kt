package com.example.orderlistapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orderlistapp.viewmodel.OrderViewModel

@Composable
fun DispatchedOrdersScreen(viewModel: OrderViewModel) {
    val dispatchedOrders by viewModel.dispatchedOrders.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDispatchedOrders() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dispatched Orders", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
            IconButton(onClick = { viewModel.loadDispatchedOrders() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF2E7D32))
            }
        }
        Spacer(Modifier.height(8.dp))

        if (dispatchedOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚚", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No dispatched orders yet", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(dispatchedOrders) { order ->
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    order.orderDate.ifEmpty { "Dispatched Date" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Text(
                                        "✅ Dispatched",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(order.customerName, fontSize = 13.sp, color = Color.DarkGray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phoneNo}"))
                                        )
                                    }
                                    .padding(2.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(order.phoneNo, fontSize = 13.sp, color = Color.DarkGray)
                            }
                            if (order.whatsapp.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val number = order.whatsapp.replace(Regex("[^0-9+]"), "")
                                            val waUri = Uri.parse("https://wa.me/$number")
                                            val wbIntent = Intent(Intent.ACTION_VIEW, waUri).apply { setPackage("com.whatsapp.w4b") }
                                            try { context.startActivity(wbIntent) }
                                            catch (e: Exception) {
                                                wbIntent.setPackage("com.whatsapp")
                                                try { context.startActivity(wbIntent) }
                                                catch (e2: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, waUri)) }
                                            }
                                        }
                                        .padding(2.dp)
                                ) {
                                    Text("💬", fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("WB: ${order.whatsapp}", fontSize = 13.sp, color = Color(0xFF25D366))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(order.address, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                            }
                            if (order.itemsDispatched.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Dispatched Items: ${order.itemsDispatched}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF388E3C),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
