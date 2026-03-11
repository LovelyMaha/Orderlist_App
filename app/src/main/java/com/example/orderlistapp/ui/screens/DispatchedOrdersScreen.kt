package com.example.orderlistapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.orderlistapp.viewmodel.OrderViewModel

// Design tokens
private val DGreen     = Color(0xFF2E7D32)
private val DGreenDark = Color(0xFF1B5E20)
private val DGreenBg   = Color(0xFFE8F5E9)
private val DNavy      = Color(0xFF1A237E)
private val DWaGreen   = Color(0xFF25D366)
private val DSubText   = Color(0xFF546E7A)
private val DDivider   = Color(0xFFECEFF1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchedOrdersScreen(viewModel: OrderViewModel) {
    val dispatchedOrders by viewModel.dispatchedOrders.collectAsState()
    val courierSelections by viewModel.courierSelections.collectAsState()
    val llrNumbers by viewModel.llrNumbers.collectAsState()
    val courierList by viewModel.courierList.collectAsState()
    val proofImages by viewModel.proofImages.collectAsState()
    val orderImagesMap by viewModel.orderImages.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDispatchedOrders() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F6FB)).padding(14.dp)) {
        // Header
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
                        .background(DGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Dispatched Orders", fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp, color = DGreen)
                    if (dispatchedOrders.isNotEmpty())
                        Text("${dispatchedOrders.size} dispatched", fontSize = 12.sp, color = DSubText)
                }
            }
            IconButton(
                onClick = { viewModel.loadDispatchedOrders() },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DGreenBg)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                    tint = DGreen, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        if (dispatchedOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚚", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No dispatched orders yet", color = DSubText, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(dispatchedOrders) { order ->
                    val context = LocalContext.current
                    val orderKey = "${order.customerName}|${order.phoneNo}"
                    val selectedCourier = courierSelections[orderKey] ?: order.courierName
                    val llrNumber = llrNumbers[orderKey] ?: order.llrNo
                    val localProofUri = proofImages[orderKey] ?: ""
                    var courierExpanded by remember { mutableStateOf(false) }
                    val uploadedImages = orderImagesMap[order.phoneNo] ?: emptyList()

                    LaunchedEffect(order.phoneNo) {
                        viewModel.loadImagesForOrder(order.phoneNo)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Date + Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    order.orderDate.ifEmpty { "Dispatched" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DGreenDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = DGreen
                                ) {
                                    Text(
                                        "✅ Dispatched",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Customer name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = DSubText, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(order.customerName, fontSize = 13.sp, color = Color.DarkGray,
                                    fontWeight = FontWeight.SemiBold)
                            }

                            // Phone — icon + number only
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phoneNo}"))
                                        )
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call",
                                    tint = DNavy, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(order.phoneNo, fontSize = 12.sp, color = Color.DarkGray,
                                    fontWeight = FontWeight.SemiBold)
                            }

                            // WhatsApp — icon + number only
                            if (order.whatsapp.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            val number = order.whatsapp.replace(Regex("[^0-9+]"), "")
                                            val waUri = Uri.parse("https://wa.me/$number")
                                            val wbIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                                setPackage("com.whatsapp.w4b")
                                            }
                                            try { context.startActivity(wbIntent) }
                                            catch (e: Exception) {
                                                wbIntent.setPackage("com.whatsapp")
                                                try { context.startActivity(wbIntent) }
                                                catch (e2: Exception) {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                                }
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("💬", fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(order.whatsapp, fontSize = 12.sp, color = DWaGreen,
                                        fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Address
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null,
                                    tint = DSubText, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(order.address, fontSize = 12.sp, color = DSubText,
                                    modifier = Modifier.weight(1f))
                            }

                            // Dispatched items
                            if (order.itemsDispatched.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Dispatched: ${order.itemsDispatched}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF388E3C),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = DDivider, thickness = 1.dp)
                            Spacer(Modifier.height(8.dp))

                            // ── Uploaded Images Grid ───────────────────────
                            if (uploadedImages.isNotEmpty()) {
                                Text("Uploaded Images:", fontSize = 11.sp, color = DSubText, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(uploadedImages) { img ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val request = ImageRequest.Builder(LocalContext.current)
                                                .data(img.image)
                                                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                                                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                                                .crossfade(true)
                                                .build()

                                            Box {
                                                AsyncImage(
                                                    model = request,
                                                    contentDescription = img.label,
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(DGreenBg),
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(
                                                    onClick = { 
                                                        viewModel.deleteImageForOrder(order.phoneNo, img.id) 
                                                        android.widget.Toast.makeText(context, "Deleting image...", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(18.dp)
                                                        .background(Color(0x99000000), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(img.label, fontSize = 9.sp, color = DSubText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(color = DDivider, thickness = 1.dp)
                                Spacer(Modifier.height(8.dp))
                            }

                            // ── Courier + LLR Row (mini compact) ────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Courier dropdown — mini
                                ExposedDropdownMenuBox(
                                    expanded = courierExpanded,
                                    onExpandedChange = { courierExpanded = it },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedCourier.ifEmpty { "Courier" },
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .menuAnchor()
                                            .weight(1f)
                                            .height(44.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selectedCourier.isEmpty()) DSubText else DGreenDark
                                        ),
                                        trailingIcon = {
                                            Icon(
                                                imageVector = if (courierExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = DSubText
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DGreen,
                                            unfocusedBorderColor = DDivider,
                                            focusedContainerColor = DGreenBg,
                                            unfocusedContainerColor = DGreenBg
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = courierExpanded,
                                        onDismissRequest = { courierExpanded = false }
                                    ) {
                                        courierList.forEach { courier ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(courier, fontSize = 10.sp,
                                                        fontWeight = if (courier == selectedCourier) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (courier == selectedCourier) DGreen else Color(0xFF263238))
                                                },
                                                onClick = {
                                                    viewModel.updateCourier(orderKey, courier)
                                                    courierExpanded = false
                                                },
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            )
                                        }
                                    }
                                }

                                // LLR booking input — mini
                                OutlinedTextField(
                                    value = llrNumber,
                                    onValueChange = { newVal -> viewModel.updateLlr(orderKey, newVal) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    placeholder = {
                                        Text("LLR No", fontSize = 9.sp, color = DSubText)
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DGreenDark
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DGreen,
                                        unfocusedBorderColor = DDivider,
                                        focusedContainerColor = DGreenBg,
                                        unfocusedContainerColor = DGreenBg
                                    )
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // ── Submit Button + WhatsApp Send Button Row ──────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Submit (💾) Button — left side
                                Button(
                                    onClick = {
                                        viewModel.updateDispatchDetails(
                                            order.customerName, order.phoneNo,
                                            selectedCourier, llrNumber
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(0.35f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DGreen)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Submit",
                                        tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Submit", fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // WhatsApp Send Button — right side
                                Button(
                                    onClick = {
                                        val msg = buildString {
                                            append("---------------------------------\n")
                                            append("Order Details:\n\n")
                                            append("Name: ${order.customerName}\n")
                                            if (order.address.isNotBlank())
                                                append("Address: ${order.address}\n")

                                            val phone = order.phoneNo.ifBlank { order.whatsapp }
                                            if (phone.isNotBlank())
                                                append("Phone: $phone\n")

                                            if (selectedCourier.isNotBlank())
                                                append("Courier: $selectedCourier\n")

                                            if (llrNumber.isNotBlank())
                                                append("Booking LLR No: $llrNumber\n")

                                            append("Status: Dispatched\n")
                                            append("---------------------------------\n")
                                        }
                                        val waNumber = order.whatsapp.ifBlank { order.phoneNo }
                                            .replace(Regex("[^0-9+]"), "")
                                        
                                        if (localProofUri.isNotBlank()) {
                                            // Send with image
                                            val imageUri = Uri.parse(localProofUri)
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/*"
                                                putExtra(Intent.EXTRA_STREAM, imageUri)
                                                putExtra(Intent.EXTRA_TEXT, msg)
                                                putExtra("jid", "$waNumber@s.whatsapp.net")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            
                                            sendIntent.setPackage("com.whatsapp.w4b")
                                            try {
                                                context.startActivity(sendIntent)
                                            } catch (e: Exception) {
                                                sendIntent.setPackage("com.whatsapp")
                                                try {
                                                    context.startActivity(sendIntent)
                                                } catch (e2: Exception) {
                                                    val chooser = Intent.createChooser(sendIntent, "Send Image")
                                                    context.startActivity(chooser)
                                                }
                                            }
                                        } else {
                                            // Text only
                                            val waUri = Uri.parse("https://wa.me/$waNumber?text=${Uri.encode(msg)}")
                                            val textIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                                setPackage("com.whatsapp.w4b")
                                            }
                                            try {
                                                context.startActivity(textIntent)
                                            } catch (e: Exception) {
                                                textIntent.setPackage("com.whatsapp")
                                                try {
                                                    context.startActivity(textIntent)
                                                } catch (e2: Exception) {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(0.65f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DWaGreen)
                                ) {
                                    Text("📭", fontSize = 13.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("WB Sent", fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // ── Courier + LLR display row (from sheet) ──────────────
                            if (order.courierName.isNotBlank() || order.llrNo.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DGreenBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null,
                                            tint = DGreen, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        if (order.courierName.isNotBlank()) {
                                            Text("Courier: ${order.courierName}", fontSize = 11.sp,
                                                color = DGreenDark, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (order.courierName.isNotBlank() && order.llrNo.isNotBlank()) {
                                            Spacer(Modifier.width(10.dp))
                                            Text("•", fontSize = 11.sp, color = DSubText)
                                            Spacer(Modifier.width(10.dp))
                                        }
                                        if (order.llrNo.isNotBlank()) {
                                            Text("LLR: ${order.llrNo}", fontSize = 11.sp,
                                                color = DGreenDark, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}
