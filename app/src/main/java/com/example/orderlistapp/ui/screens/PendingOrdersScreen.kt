package com.example.orderlistapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orderlistapp.viewmodel.OrderViewModel

// ── Design Tokens (mirrors ActiveOrdersScreen) ───────────────────────────────
private val PNavy       = Color(0xFF1A237E)
private val PNavyLight  = Color(0xFF3949AB)
private val PNavyBg     = Color(0xFFE8EAF6)
private val PGreen      = Color(0xFF2E7D32)
private val PGreenLight = Color(0xFFE8F5E9)
private val POrange     = Color(0xFFE65100)
private val POrangeBg   = Color(0xFFFFF3E0)
private val PPurple     = Color(0xFF6A1B9A)
private val PPurpleBg   = Color(0xFFF3E5F5)
private val PWaGreen    = Color(0xFF25D366)
private val PSubText    = Color(0xFF546E7A)
private val PDivider    = Color(0xFFECEFF1)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingOrdersScreen(viewModel: OrderViewModel) {
    val pendingFullOrders   by viewModel.pendingFullOrders.collectAsState()
    val pendingMissingItems by viewModel.pendingMissingItems.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPendingFullOrders()
        viewModel.loadPendingMissingItems()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FB))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ── Screen Header ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(POrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Pending Orders", fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp, color = POrange)
                    val total = pendingFullOrders.size + pendingMissingItems.size
                    if (total > 0)
                        Text("$total orders awaiting", fontSize = 12.sp, color = PSubText)
                }
            }
            IconButton(
                onClick = {
                    viewModel.loadPendingFullOrders()
                    viewModel.loadPendingMissingItems()
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(POrangeBg)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                    tint = POrange, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        if (pendingFullOrders.isEmpty() && pendingMissingItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 52.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("No pending orders!", color = PSubText, fontSize = 16.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // ══ Section 1: Full Order Pending ═════════════════════════
                if (pendingFullOrders.isNotEmpty()) {
                    item {
                        SectionHeader(
                            icon = Icons.Default.Warning,
                            label = "Full Order Pending",
                            count = pendingFullOrders.size,
                            accentColor = POrange
                        )
                    }
                    items(pendingFullOrders) { order ->
                        val pendingList = remember(order) {
                            val list = order.itemsPending.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (list.isEmpty()) order.itemsOrdered.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            else list
                        }
                        PendingOrderCard(
                            customerName = order.customerName,
                            phoneNo      = order.phoneNo,
                            whatsapp     = order.whatsapp,
                            address      = order.address,
                            orderDate    = order.orderDate,
                            pendingList  = pendingList,
                            accentColor  = POrange,
                            cardBg       = POrangeBg,
                            onDispatch   = { dispatchedItems, stillPendingItems ->
                                viewModel.markDispatchedFromPending(
                                    customerName   = order.customerName,
                                    phoneNo        = order.phoneNo,
                                    dispatchedItems = dispatchedItems,
                                    pendingItems   = stillPendingItems
                                )
                            }
                        )
                    }
                }

                // ══ Section 2: Missing Items Pending ═════════════════════
                if (pendingMissingItems.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        SectionHeader(
                            icon = Icons.Default.RemoveShoppingCart,
                            label = "Missing Items",
                            count = pendingMissingItems.size,
                            accentColor = PPurple
                        )
                    }
                    items(pendingMissingItems) { order ->
                        val pendingList = remember(order) {
                            order.itemsPending.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                        PendingOrderCard(
                            customerName = order.customerName,
                            phoneNo      = order.phoneNo,
                            whatsapp     = order.whatsapp,
                            address      = order.address,
                            orderDate    = order.orderDate,
                            pendingList  = pendingList,
                            accentColor  = PPurple,
                            cardBg       = PPurpleBg,
                            onDispatch   = { dispatchedItems, stillPendingItems ->
                                viewModel.markDispatchedFromPending(
                                    customerName   = order.customerName,
                                    phoneNo        = order.phoneNo,
                                    dispatchedItems = dispatchedItems,
                                    pendingItems   = stillPendingItems
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accentColor,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = accentColor
        ) {
            Text(
                "$count",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Unified Pending Order Card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingOrderCard(
    customerName: String,
    phoneNo: String,
    whatsapp: String,
    address: String,
    orderDate: String,
    pendingList: List<String>,
    accentColor: Color,
    cardBg: Color,
    onDispatch: (dispatchedItems: String, stillPendingItems: String) -> Unit
) {
    var expanded       by remember { mutableStateOf(false) }
    var readyItems     by remember(customerName) { mutableStateOf(setOf<String>()) }
    var showDialog     by remember { mutableStateOf(false) }

    val allReady  = pendingList.isNotEmpty() && pendingList.all { it in readyItems }
    val noneReady = readyItems.isEmpty()

    // ── Dispatch Confirmation Dialog ───────────────────────────────────────
    if (showDialog) {
        val dispatching  = readyItems.toList()
        val stillPending = pendingList.filter { it !in readyItems }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null,
                        tint = accentColor, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dispatch Summary", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dispatching.isNotEmpty()) {
                        Text("✅ Will Dispatch:", fontWeight = FontWeight.SemiBold,
                            color = PGreen, fontSize = 13.sp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            dispatching.forEach { item ->
                                Surface(shape = RoundedCornerShape(8.dp), color = PGreen) {
                                    Text(item, color = Color.White, fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                    if (stillPending.isNotEmpty()) {
                        Text("🔴 Still Pending:", fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD32F2F), fontSize = 13.sp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            stillPending.forEach { item ->
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD32F2F)) {
                                    Text(item, color = Color.White, fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDispatch(
                            dispatching.joinToString(","),
                            stillPending.joinToString(",")
                        )
                        showDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PGreen)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Confirm Dispatch", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Card ───────────────────────────────────────────────────────────────
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // ── Collapsed Header ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Date badge (top-right)
                if (orderDate.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                orderDate,
                                fontSize = 10.sp,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // ── Name (left) | Phone + WA (right) ─────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left: avatar + name
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                customerName.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = accentColor
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Name", fontSize = 10.sp, color = PSubText,
                                fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                            Text(
                                customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF263238),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (address.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    address,
                                    fontSize = 11.sp,
                                    color = PSubText,
                                    maxLines = 2,
                                    lineHeight = 16.sp,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right: Phone + WhatsApp — icons only, no labels
                    Column(horizontalAlignment = Alignment.End) {
                        val context = LocalContext.current
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNo"))
                                    )
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call",
                                tint = PNavy, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                phoneNo,
                                fontSize = 12.sp, color = Color(0xFF37474F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (whatsapp.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val number = whatsapp.replace(Regex("[^0-9+]"), "")
                                        val waUri = Uri.parse("https://wa.me/$number")
                                        val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                            setPackage("com.whatsapp.w4b")
                                        }
                                        try { context.startActivity(intent) }
                                        catch (e: Exception) {
                                            intent.setPackage("com.whatsapp")
                                            try { context.startActivity(intent) }
                                            catch (e2: Exception) {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                            }
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("💬", fontSize = 13.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    whatsapp,
                                    fontSize = 12.sp, color = PWaGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = PDivider, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                // ── Pending items chips ───────────────────────────────────
                Row(verticalAlignment = Alignment.Top) {
                    Text("Order :", fontSize = 12.sp, color = PSubText,
                        fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp)
                    ) {
                        pendingList.forEach { item ->
                            val isReady = item in readyItems
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isReady) PGreenLight else accentColor.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    item,
                                    fontSize = 12.sp,
                                    color = if (isReady) PGreen else accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Expand indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(shape = RoundedCornerShape(50), color = PDivider) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (expanded) "Collapse" else "Tap to update",
                                fontSize = 11.sp, color = PSubText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                              else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = PSubText, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Expanded Body ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)),
                exit  = shrinkVertically(animationSpec = tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FE))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    HorizontalDivider(color = PDivider)
                    Spacer(Modifier.height(12.dp))

                    // ── Packing Checklist ─────────────────────────────────
                    if (pendingList.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory, contentDescription = null,
                                    tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Packing Checklist",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = accentColor)
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when {
                                    allReady  -> PGreen
                                    !noneReady -> accentColor
                                    else      -> Color(0xFFECEFF1)
                                }
                            ) {
                                Text(
                                    "${readyItems.size} / ${pendingList.size} ready",
                                    fontSize = 11.sp,
                                    color = if (!noneReady) Color.White else PSubText,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("📦 Tap items you NOW have in stock:",
                            fontSize = 12.sp, color = PSubText,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))

                        pendingList.forEach { item ->
                            val isReady   = item in readyItems
                            val rowBg     = if (isReady) PGreenLight else Color(0xFFF1F3F8)
                            val borderClr = if (isReady) Color(0xFF81C784) else Color(0xFFCFD8DC)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, borderClr, RoundedCornerShape(12.dp))
                                    .background(rowBg)
                                    .clickable {
                                        readyItems = if (isReady) readyItems - item
                                                     else readyItems + item
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isReady) PGreen else Color.White)
                                        .border(1.5.dp,
                                            if (isReady) PGreen else Color(0xFFB0BEC5),
                                            RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isReady)
                                        Icon(Icons.Default.Check, contentDescription = null,
                                            tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = item,
                                    fontSize = 14.sp,
                                    color = if (isReady) Color(0xFF388E3C) else Color(0xFF263238),
                                    fontWeight = if (isReady) FontWeight.Medium else FontWeight.Normal,
                                    textDecoration = if (isReady)
                                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    else null,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isReady)
                                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                                        tint = PGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // ── Dispatch Button ───────────────────────────────────
                    Button(
                        onClick = { showDialog = true },
                        enabled = !noneReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (allReady) PGreen else accentColor,
                            disabledContainerColor = Color(0xFFB0BEC5)
                        )
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                noneReady -> "Select items to dispatch"
                                allReady  -> "Dispatch All Items ✓"
                                else      -> "Dispatch (${readyItems.size} ready)"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
