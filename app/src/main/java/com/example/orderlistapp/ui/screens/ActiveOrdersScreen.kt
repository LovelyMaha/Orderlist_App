package com.example.orderlistapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.orderlistapp.data.model.Order
import com.example.orderlistapp.data.model.UnifiedOrder
import com.example.orderlistapp.viewmodel.OrderViewModel
import java.io.File

// ── Design Tokens ───────────────────────────────────────────────────────────
private val Navy       = Color(0xFF1A237E)
private val NavyLight  = Color(0xFF3949AB)
private val NavyBg     = Color(0xFFE8EAF6)
private val Green      = Color(0xFF2E7D32)
private val GreenLight = Color(0xFFE8F5E9)
private val Orange     = Color(0xFFE65100)
private val OrangeBg   = Color(0xFFFFF3E0)
private val WaGreen    = Color(0xFF25D366)
private val CardBg     = Color(0xFFFFFFFF)
private val SubText    = Color(0xFF546E7A)
private val DividerClr = Color(0xFFECEFF1)
private val Highlight  = Color(0xFFFFEB3B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveOrdersScreen(viewModel: OrderViewModel, isAdmin: Boolean) {
    val orders        by viewModel.activeOrders.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val searchQuery   by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val focusManager  = LocalFocusManager.current

    // Launching `loadAllData()` in MainDashboard handles initialization.
    // We removed redundant `viewModel.loadActiveOrders()` here to prevent duplicate API hits.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FB))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
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
                        .background(Navy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Active Orders", fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp, color = Navy)
                    if (!isLoading && orders.isNotEmpty() && searchQuery.isBlank())
                        Text("${orders.size} orders pending", fontSize = 12.sp, color = SubText)
                    if (searchQuery.isNotBlank())
                        Text("${searchResults.size} result(s) found", fontSize = 12.sp, color = NavyLight)
                }
            }
            IconButton(
                onClick = { viewModel.loadActiveOrders() },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavyBg)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                    tint = Navy, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Search Bar ─────────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            placeholder = {
                Text("Search name, phone, WB number, items…",
                    fontSize = 13.sp, color = SubText)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search",
                    tint = Navy, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        viewModel.updateSearchQuery("")
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear",
                            tint = SubText, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Navy,
                unfocusedBorderColor = DividerClr,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(14.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Navy)
            }

            // ── Search Mode ──────────────────────────────────────────────
            searchQuery.isNotBlank() -> {
                if (searchResults.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("No results for \"$searchQuery\"",
                                color = SubText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(searchResults, key = { it.phoneNo + "_" + it.orderDate + "_" + it.source }) { result ->
                            UnifiedOrderCard(order = result, searchQuery = searchQuery)
                        }
                    }
                }
            }

            // ── Normal Mode ───────────────────────────────────────────────
            orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 52.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("No active orders", color = SubText, fontSize = 16.sp,
                        fontWeight = FontWeight.Medium)
                }
            }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(orders, key = { it.phoneNo + "_" + it.orderDate }) { order ->
                    OrderCard(order = order, viewModel = viewModel)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  UnifiedOrderCard — displayed during search
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnifiedOrderCard(order: UnifiedOrder, searchQuery: String) {
    val context = LocalContext.current
    val q = searchQuery.trim().lowercase()

    val (badgeColor, badgeBg) = when (order.source) {
        "Active"     -> Pair(Navy, NavyBg)
        "Pending",
        "Missing"    -> Pair(Orange, OrangeBg)
        "Dispatched" -> Pair(Green, GreenLight)
        else         -> Pair(SubText, DividerClr)
    }

    val displayItems = remember(order) { order.getDisplayItems() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // ── Source Badge + Date ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                    Text(
                        "● ${order.source}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (order.orderDate.isNotBlank()) {
                    Text(order.orderDate, fontSize = 10.sp, color = SubText)
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DividerClr, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // ── Name Row ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavyBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        order.customerName.take(1).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Navy
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Name", fontSize = 10.sp, color = SubText,
                        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    Text(
                        buildHighlightedText(order.customerName, q),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Navy
                    )
                }
            }

            // ── Address ───────────────────────────────────────────────────
            if (order.address.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        tint = SubText, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text("Address", fontSize = 10.sp, color = SubText,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                        Text(
                            text = order.address,
                            fontSize = 13.sp,
                            color = SubText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Phone + WhatsApp Row ───────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                if (order.phoneNo.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyBg)
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL,
                                    Uri.parse("tel:${order.phoneNo}"))
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call",
                            tint = Navy, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            buildHighlightedText(order.phoneNo, q),
                            fontSize = 13.sp,
                            color = Navy,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (order.whatsapp.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .clickable {
                                val number = order.whatsapp.replace(Regex("[^0-9+]"), "")
                                val waUri = Uri.parse("https://wa.me/$number")
                                val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                    setPackage("com.whatsapp.w4b")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    intent.setPackage("com.whatsapp")
                                    try {
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                    }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("💬", fontSize = 14.sp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            buildHighlightedText(order.whatsapp, q),
                            fontSize = 13.sp,
                            color = WaGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerClr, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // ── Items ──────────────────────────────────────────────────────
            Text("Order :", fontSize = 11.sp, color = SubText,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayItems.forEach { item ->
                    val isMatch = item.lowercase().contains(q)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMatch) Highlight else NavyBg,
                        border = if (isMatch) androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFF57F17)) else null
                    ) {
                        Text(
                            text = buildHighlightedText(item, q),
                            fontSize = 12.sp,
                            color = if (isMatch) Color(0xFF5D4037) else NavyLight,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Highlighted text helper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun buildHighlightedText(text: String, query: String) =
    buildAnnotatedString {
        if (query.isBlank()) { append(text); return@buildAnnotatedString }
        val lower = text.lowercase()
        val q = query.lowercase()
        var start = 0
        while (true) {
            val idx = lower.indexOf(q, start)
            if (idx == -1) { append(text.substring(start)); break }
            append(text.substring(start, idx))
            withStyle(SpanStyle(
                background = Highlight,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF5D4037)
            )) {
                append(text.substring(idx, idx + q.length))
            }
            start = idx + q.length
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
//  OrderCard — Active orders only (packing + dispatch/pending + WhatsApp)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrderCard(order: Order, viewModel: OrderViewModel) {
    var showDetails         by remember { mutableStateOf(false) }
    var showConfirmDispatch by remember { mutableStateOf(false) }
    var showConfirmPending  by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val itemList = remember(order) { order.getItemList() }

    var checkedItems by remember(order) {
        mutableStateOf(itemList.associateWith { false }.toMutableMap())
    }

    val allPacked   = itemList.isNotEmpty() && itemList.all { checkedItems[it] == true }
    val nonePacked  = itemList.all { checkedItems[it] == false }
    val packedCount = itemList.count { checkedItems[it] == true }

    // ── Image Upload States ────────────────────────────────────────────────
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageLabel by remember { mutableStateOf("") }
    val isUploading by viewModel.isUploadingImage.collectAsState()
    val orderImagesMapState = viewModel.orderImages.collectAsState()
    val uploadedImages by remember(order.phoneNo) {
        androidx.compose.runtime.derivedStateOf { orderImagesMapState.value[order.phoneNo] ?: emptyList() }
    }
    var showImageSourceSheet by remember { mutableStateOf(false) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery launcher — auto-label: yyyy-MM-dd + phoneNo
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            val today = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            imageLabel = "${today}_${order.phoneNo}"
        }
    }
    // Camera launcher — auto-label: yyyy-MM-dd + phoneNo
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            val today = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            imageLabel = "${today}_${order.phoneNo}"
        }
    }

    LaunchedEffect(showDetails) {
        if (showDetails) {
            viewModel.loadImagesForOrder(order.phoneNo)
        }
    }

    // ── Confirm Dispatch Dialog ────────────────────────────────────────────
    if (showConfirmDispatch) {
        val dispatched = itemList.filter { checkedItems[it] == true }.joinToString(",")
        val pending    = itemList.filter { checkedItems[it] == false }.joinToString(",")
        AlertDialog(
            onDismissRequest = { showConfirmDispatch = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null,
                        tint = Navy, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (pending.isNotEmpty()) "Partial Dispatch" else "Dispatch Order",
                        fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    if (pending.isNotEmpty())
                        "Dispatch $packedCount checked item(s) and move the rest to Pending?"
                    else "Mark all items as Dispatched?",
                    fontSize = 14.sp, color = SubText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markDispatched(
                            customerName    = order.customerName,
                            phoneNo         = order.phoneNo,
                            dispatchedItems = dispatched,
                            pendingItems    = pending
                        )
                        showConfirmDispatch = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dispatch", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDispatch = false }) { Text("Cancel") }
            }
        )
    }

    // ── Confirm Full Pending Dialog ────────────────────────────────────────
    if (showConfirmPending) {
        AlertDialog(
            onDismissRequest = { showConfirmPending = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory, contentDescription = null,
                        tint = Orange, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Move to Pending?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text("Move entire order for \"${order.customerName}\" to Pending?",
                    fontSize = 14.sp, color = SubText)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markFullPending(order.customerName, order.phoneNo)
                        showConfirmPending = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Move to Pending", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPending = false }) { Text("Cancel") }
            }
        )
    }

    // ── Card ──────────────────────────────────────────────────────────────
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column {
            // ── Collapsed Header (always visible) ──────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = !showDetails }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // ── Row 1: Name (left) | Phone + WA (right) ───────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NavyBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = order.customerName.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Navy
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Name",
                                fontSize = 10.sp,
                                color = SubText,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                order.customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Navy
                            )
                            if (order.address.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    order.address,
                                    fontSize = 11.sp,
                                    color = SubText,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Right: Phone + WA stacked
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL,
                                        Uri.parse("tel:${order.phoneNo}"))
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call",
                                tint = Navy, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                order.phoneNo,
                                fontSize = 12.sp,
                                color = Color(0xFF37474F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (order.whatsapp.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val number = order.whatsapp.replace(Regex("[^0-9+]"), "")
                                        val waUri = Uri.parse("https://wa.me/$number")
                                        val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                            setPackage("com.whatsapp.w4b")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
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
                                    order.whatsapp,
                                    fontSize = 12.sp,
                                    color = WaGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = DividerClr, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                // ── Row 2: Order label + item chips ───────────────────────
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "Order :",
                        fontSize = 12.sp,
                        color = SubText,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemList.forEach { item ->
                            val isPacked = checkedItems[item] == true
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPacked) GreenLight else NavyBg
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 12.sp,
                                    color = if (isPacked) Green else NavyLight,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Expand/Collapse arrow ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = DividerClr,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showDetails) "Collapse" else "Tap to pack",
                                fontSize = 11.sp,
                                color = SubText,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showDetails) Icons.Default.KeyboardArrowUp
                                              else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = SubText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Expanded Body ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically(animationSpec = tween(220)),
                exit  = shrinkVertically(animationSpec = tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FE))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    HorizontalDivider(color = DividerClr)
                    Spacer(Modifier.height(12.dp))

                    // ── Image Upload & Display Section ─────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Images", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy)
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { showImageSourceSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(NavyBg, CircleShape)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add Image", tint = Navy, modifier = Modifier.size(18.dp))
                        }
                    }

                    // ── Camera / Gallery chooser bottom sheet ─────────────────
                    if (showImageSourceSheet) {
                        AlertDialog(
                            onDismissRequest = { showImageSourceSheet = false },
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text("Select Image Source", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Camera option
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NavyBg)
                                            .clickable {
                                                showImageSourceSheet = false
                                                val tmpFile = File.createTempFile("cam_", ".jpg",
                                                    context.externalCacheDir ?: context.cacheDir)
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    tmpFile
                                                )
                                                cameraImageUri = uri
                                                cameraLauncher.launch(uri)
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null,
                                            tint = Navy, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Camera", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy)
                                            Text("Take a new photo", fontSize = 11.sp, color = SubText)
                                        }
                                    }
                                    // Gallery option
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NavyBg)
                                            .clickable {
                                                showImageSourceSheet = false
                                                galleryLauncher.launch("image/*")
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                                            tint = Navy, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy)
                                            Text("Choose from gallery", fontSize = 11.sp, color = SubText)
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(onClick = { showImageSourceSheet = false }) {
                                    Text("Cancel", color = SubText)
                                }
                            }
                        )
                    }

                    // Preview selected image before uploading
                    if (selectedImageUri != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, DividerClr, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DividerClr),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = imageLabel,
                                    onValueChange = { imageLabel = it },
                                    label = { Text("Label (e.g. Sample 1)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (imageLabel.isNotBlank()) {
                                                viewModel.uploadImageForOrder(context.applicationContext, order.phoneNo, imageLabel, selectedImageUri!!) { success ->
                                                    android.widget.Toast.makeText(context, viewModel.message.value, android.widget.Toast.LENGTH_SHORT).show()
                                                    if (success) {
                                                        selectedImageUri = null
                                                        imageLabel = ""
                                                    }
                                                }
                                            }
                                        },
                                        enabled = imageLabel.isNotBlank() && !isUploading,
                                        modifier = Modifier.height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Navy)
                                    ) {
                                        if (isUploading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text("Upload", fontSize = 12.sp)
                                        }
                                    }
                                    TextButton(onClick = { selectedImageUri = null }, modifier = Modifier.height(36.dp)) {
                                        Text("Cancel", fontSize = 12.sp, color = Orange)
                                    }
                                }
                            }
                        }
                    }

                    // Display fetched images
                    if (uploadedImages.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uploadedImages) { img ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val currentContext = LocalContext.current
                                    val request = remember(img.image) {
                                        ImageRequest.Builder(currentContext)
                                            .data(img.image)
                                            .addHeader("User-Agent", "Mozilla/5.0")
                                            .addHeader("Accept", "image/*,*/*;q=0.8")
                                            .crossfade(true)
                                            .build()
                                    }

                                    Box {
                                        AsyncImage(
                                            model = request,
                                            contentDescription = img.label,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NavyBg)
                                                .clickable { fullScreenImageUrl = img.image },
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteImageForOrder(order.phoneNo, img.id) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                                .size(22.dp)
                                                .background(Color(0x99000000), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(img.label, fontSize = 11.sp, color = SubText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // ── Full-screen image dialog ───────────────────────────
                    fullScreenImageUrl?.let { imgUrl ->
                        Dialog(
                            onDismissRequest = { fullScreenImageUrl = null },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xE6000000))
                                    .clickable { fullScreenImageUrl = null },
                                contentAlignment = Alignment.Center
                            ) {
                                val currentContext = LocalContext.current
                                val fullReq = remember(imgUrl) {
                                    ImageRequest.Builder(currentContext)
                                        .data(imgUrl)
                                        .addHeader("User-Agent", "Mozilla/5.0")
                                        .addHeader("Accept", "image/*,*/*;q=0.8")
                                        .crossfade(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = fullReq,
                                    contentDescription = "Full image",
                                    modifier = Modifier
                                        .fillMaxWidth(0.95f)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                                // Close button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(20.dp)
                                        .size(36.dp)
                                        .background(Color(0x99000000), CircleShape)
                                        .clickable { fullScreenImageUrl = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close",
                                        tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = DividerClr)
                    Spacer(Modifier.height(12.dp))

                    // ── Packing Checklist ──────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory, contentDescription = null,
                                tint = Navy, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Packing Checklist", fontWeight = FontWeight.Bold,
                                fontSize = 13.sp, color = Navy)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (allPacked) Green else if (nonePacked) Color(0xFFECEFF1) else Orange
                        ) {
                            Text(
                                "$packedCount / ${itemList.size}",
                                fontSize = 10.sp,
                                color = if (allPacked || !nonePacked) Color.White else SubText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemList.forEach { item ->
                            val isPacked = checkedItems[item] == true
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPacked) GreenLight else Color(0xFFF1F3F8),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isPacked) Color(0xFF81C784) else Color(0xFFCFD8DC)
                                ),
                                modifier = Modifier.clickable {
                                    checkedItems = checkedItems
                                        .toMutableMap()
                                        .also { it[item] = !isPacked }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isPacked) Green else Color.White)
                                            .border(
                                                1.dp,
                                                if (isPacked) Green else Color(0xFFB0BEC5),
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPacked)
                                            Icon(Icons.Default.Check, contentDescription = null,
                                                tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = item,
                                        fontSize = 12.sp,
                                        color = if (isPacked) Color(0xFF388E3C) else Color(0xFF263238),
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = if (isPacked) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── Action Buttons ─────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmPending = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Orange),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Orange,
                                containerColor = OrangeBg
                            )
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pending", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showConfirmDispatch = true },
                            enabled = !nonePacked,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (allPacked) Green else Navy,
                                disabledContainerColor = Color(0xFFB0BEC5)
                            )
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null,
                                tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (allPacked) "Dispatch All"
                                else if (!nonePacked) "Dispatch ($packedCount)"
                                else "Dispatch",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── WhatsApp Send Button ─────────────────────────────────
                    Button(
                        onClick = {
                            val status = when {
                                allPacked  -> "All Dispatched"
                                nonePacked -> "Undispatched"
                                else       -> "Partially Dispatched"
                            }
                            val msg = buildString {
                                append("---------------------------------\n")
                                append("Order Details:\n\n")
                                append("Name: ${order.customerName}\n")
                                if (order.address.isNotBlank())
                                    append("Address: ${order.address}\n")
                                val phone = order.phoneNo.ifBlank { order.whatsapp }
                                if (phone.isNotBlank())
                                    append("Phone: $phone\n")
                                append("Status: $status\n")
                                append("---------------------------------\n")
                            }
                            val waNumber = order.whatsapp.ifBlank { order.phoneNo }
                                .replace(Regex("[^0-9+]"), "")
                            val waUri = Uri.parse("https://wa.me/$waNumber?text=${Uri.encode(msg)}")
                            val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                setPackage("com.whatsapp.w4b")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                intent.setPackage("com.whatsapp")
                                try { context.startActivity(intent) }
                                catch (e2: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WaGreen)
                    ) {
                        Text("\uD83D\uDCE8", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("Send via WhatsApp", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
