package com.example.orderlistapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.orderlistapp.viewmodel.OrderViewModel
import kotlinx.coroutines.launch
import java.io.File

// ── Design tokens ────────────────────────────────────────────────────────────
private val DGreen     = Color(0xFF2E7D32)
private val DGreenDark = Color(0xFF1B5E20)
private val DGreenBg   = Color(0xFFE8F5E9)
private val DNavy      = Color(0xFF1A237E)
private val DWaGreen   = Color(0xFF25D366)
private val DSubText   = Color(0xFF546E7A)
private val DDivider   = Color(0xFFECEFF1)
private val DRedDark   = Color(0xFFB71C1C)
private val DRedBg     = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchedOrdersScreen(viewModel: OrderViewModel) {
    val dispatchedOrders    by viewModel.dispatchedOrders.collectAsState()
    val courierSelections   by viewModel.courierSelections.collectAsState()
    val llrNumbers          by viewModel.llrNumbers.collectAsState()
    val courierList         by viewModel.courierList.collectAsState()
    val dispatchStatusMap   by viewModel.dispatchStatusMap.collectAsState()
    val isDeletingDispatched by viewModel.isDeletingDispatched.collectAsState()

    // Sort newest dispatched first
    val sortedDispatchedOrders = remember(dispatchedOrders, dispatchStatusMap) {
        dispatchedOrders.sortedByDescending { dispatchStatusMap[it.phoneNo]?.second ?: 0L }
    }

    // Delete-dialog state
    var showDeleteDialog      by remember { mutableStateOf(false) }
    var showDeleteConfirm     by remember { mutableStateOf(false) }
    var pendingDeleteFilter   by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadDispatchedOrders() }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF4F6FB))
        .padding(14.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Dispatched Orders", fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp, color = DGreen)
                    if (dispatchedOrders.isNotEmpty())
                        Text("${dispatchedOrders.size} dispatched",
                            fontSize = 13.sp, color = DSubText)
                }
            }

            // Right: Refresh + Delete
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Refresh
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

                // Delete
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DRedBg)
                        .size(40.dp)
                ) {
                    if (isDeletingDispatched) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = DRedDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = DRedDark, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Filter Selection Dialog ─────────────────────────────────────────
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                            tint = DRedDark, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Dispatched Orders",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp, color = DRedDark)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select which orders to delete from the list:",
                            fontSize = 14.sp, color = DSubText)

                        // Previous Day option
                        DeleteOptionRow(
                            icon   = Icons.Default.Today,
                            label  = "Previous Day",
                            desc   = "Remove orders dispatched before today",
                            onClick = {
                                pendingDeleteFilter = "previousDay"
                                showDeleteDialog = false
                                showDeleteConfirm = true
                            }
                        )

                        // Previous Week option
                        DeleteOptionRow(
                            icon   = Icons.Default.DateRange,
                            label  = "Previous Week",
                            desc   = "Remove orders older than 7 days",
                            onClick = {
                                pendingDeleteFilter = "previousWeek"
                                showDeleteDialog = false
                                showDeleteConfirm = true
                            }
                        )

                        // All option
                        DeleteOptionRow(
                            icon   = Icons.Default.DeleteSweep,
                            label  = "All Orders",
                            desc   = "Clear the entire dispatched list",
                            tint   = DRedDark,
                            bgColor = DRedBg,
                            onClick = {
                                pendingDeleteFilter = "all"
                                showDeleteDialog = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = DSubText, fontSize = 14.sp)
                    }
                }
            )
        }

        // ── Confirmation Dialog ─────────────────────────────────────────────
        if (showDeleteConfirm) {
            val filterLabel = when (pendingDeleteFilter) {
                "previousDay"  -> "orders dispatched before today"
                "previousWeek" -> "orders older than 7 days"
                "all"          -> "ALL dispatched orders"
                else           -> "selected orders"
            }
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("Confirm Delete", fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp, color = DRedDark)
                },
                text = {
                    Text("Are you sure you want to delete $filterLabel from this list?\n\nThis only removes them from the current view — server data is not affected.",
                        fontSize = 14.sp, color = Color(0xFF37474F))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteDispatchedOrders(pendingDeleteFilter)
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DRedDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = DSubText, fontSize = 14.sp)
                    }
                }
            )
        }

        // ── Empty State ────────────────────────────────────────────────────
        if (dispatchedOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚚", fontSize = 52.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("No dispatched orders yet",
                        color = DSubText, fontSize = 17.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sortedDispatchedOrders, key = { it.phoneNo + "_" + it.orderDate }) { order ->
                    DispatchedOrderCard(order = order, viewModel = viewModel,
                        courierSelections = courierSelections,
                        llrNumbers = llrNumbers,
                        courierList = courierList,
                        dispatchStatusMap = dispatchStatusMap)
                }
            }
        }
    }
}

// ── Reusable delete option row ──────────────────────────────────────────────
@Composable
private fun DeleteOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    desc: String,
    tint: Color = DGreen,
    bgColor: Color = DGreenBg,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null,
            tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = tint)
            Text(desc, fontSize = 12.sp, color = DSubText)
        }
    }
}

// ── Single dispatched order card ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DispatchedOrderCard(
    order: com.example.orderlistapp.data.model.DispatchedOrder,
    viewModel: OrderViewModel,
    courierSelections: Map<String, String>,
    llrNumbers: Map<String, String>,
    courierList: List<String>,
    dispatchStatusMap: Map<String, Pair<String, Long>>
) {
    val context = LocalContext.current
    val orderKey = "${order.customerName}|${order.phoneNo}"
    val selectedCourier = courierSelections[orderKey] ?: order.courierName
    val llrNumber = llrNumbers[orderKey] ?: order.llrNo
    var courierExpanded by remember { mutableStateOf(false) }

    val orderImagesMapState = viewModel.orderImages.collectAsState()
    val uploadedImages by remember(order.phoneNo) {
        androidx.compose.runtime.derivedStateOf {
            orderImagesMapState.value[order.phoneNo] ?: emptyList()
        }
    }

    // State for tracking selected image IDs
    var selectedImages by remember { mutableStateOf<Set<String>>(emptySet()) }

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var imageLabel      by remember { mutableStateOf("") }
    val isUploading     by viewModel.isUploadingImage.collectAsState()
    var showImageSourceSheet by remember { mutableStateOf(false) }
    var fullScreenImageUrl   by remember { mutableStateOf<String?>(null) }
    var cameraImageUri       by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSendingWb          by remember { mutableStateOf(false) }

    val msgStatus = dispatchStatusMap[order.phoneNo]?.first ?: "NONE"

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedImageUri = uri; imageLabel = "" }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) { selectedImageUri = cameraImageUri; imageLabel = "" }
    }

    // Removed showDetails state; all content is permanently visible
    LaunchedEffect(order.phoneNo) {
        viewModel.loadImagesForOrder(order.phoneNo)
    }

    // When images are loaded/deleted, keep selection consistent
    LaunchedEffect(uploadedImages) {
        val currentImageIds = uploadedImages.map { it.id }.toSet()
        selectedImages = selectedImages.intersect(currentImageIds)
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Date + Badge row ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format the ISO date nicely, e.g., "2026-03-01T22:53:08.673Z" -> "2026-03-01 22:53"
                val displayDate = remember(order.orderDate) {
                    if (order.orderDate.contains("T")) {
                        val parts = order.orderDate.split("T")
                        val datePart = parts[0]
                        val timePart = parts.getOrNull(1)?.substringBefore(".")?.take(5) ?: ""
                        "$datePart $timePart"
                    } else {
                        order.orderDate
                    }
                }

                Text(
                    displayDate.ifEmpty { "Dispatched" },
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = DGreenDark,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    // WA dispatch status dot
                    if (msgStatus != "NONE") {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(
                                    if (msgStatus == "SENT") Color(0xFF2E7D32)
                                    else Color(0xFFC62828)
                                )
                        )
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = DGreen) {
                        Text(
                            "✅ Dispatched",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color    = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Customer Name ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null,
                    tint = DSubText, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(order.customerName,
                    fontSize   = 17.sp,
                    color      = Color.DarkGray,
                    fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(4.dp))

            // ── Phone ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phoneNo}"))
                        )
                    }
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Call",
                    tint = DNavy, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(order.phoneNo,
                    fontSize   = 15.sp,
                    color      = Color.DarkGray,
                    fontWeight = FontWeight.SemiBold)
            }

            // ── WhatsApp ─────────────────────────────────────────────────
            if (order.whatsapp.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val number = order.whatsapp.replace(Regex("[^0-9+]"), "")
                            val waUri  = Uri.parse("https://wa.me/$number")
                            val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                setPackage("com.whatsapp.w4b")
                            }
                            try { context.startActivity(intent) }
                            catch (e: Exception) {
                                intent.setPackage("com.whatsapp")
                                try { context.startActivity(intent) }
                                catch (e2: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, waUri)) }
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                ) {
                    Text("💬", fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(order.whatsapp,
                        fontSize   = 14.sp,
                        color      = DWaGreen,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Address ──────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    tint = DSubText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(order.address,
                    fontSize = 14.sp,
                    color    = DSubText,
                    modifier = Modifier.weight(1f))
            }

            // ── Items Dispatched ─────────────────────────────────────────
            if (order.itemsDispatched.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Dispatched: ${order.itemsDispatched}",
                    fontSize   = 14.sp,
                    color      = Color(0xFF388E3C),
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Expanded Body Content (Now Flat) ──────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = DDivider, thickness = 1.dp)
                Spacer(Modifier.height(14.dp))

                // ── Images Section ─────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Images",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                            color      = DGreen)
                        if (uploadedImages.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "(Tap to select, long press to view)",
                                fontSize = 10.sp,
                                color = DSubText,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        IconButton(
                            onClick  = { showImageSourceSheet = true },
                            modifier = Modifier
                                .size(34.dp)
                                .background(DGreenBg, CircleShape)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add Image",
                                tint = DGreen, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Camera / Gallery chooser dialog
                    if (showImageSourceSheet) {
                        AlertDialog(
                            onDismissRequest = { showImageSourceSheet = false },
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text("Select Image Source",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 16.sp,
                                    color      = DGreen)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DGreenBg)
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
                                            tint = DGreen, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Camera", fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp, color = DGreen)
                                            Text("Take a new photo", fontSize = 12.sp, color = DSubText)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DGreenBg)
                                            .clickable {
                                                showImageSourceSheet = false
                                                galleryLauncher.launch("image/*")
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                                            tint = DGreen, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Gallery", fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp, color = DGreen)
                                    Text("Choose from gallery", fontSize = 12.sp, color = DSubText)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showImageSourceSheet = false }) {
                            Text("Cancel", color = DSubText, fontSize = 14.sp)
                        }
                    }
                )
            }

            // Preview selected image before upload
            if (selectedImageUri != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Preview",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DDivider),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value       = imageLabel,
                            onValueChange = { imageLabel = it },
                            label       = { Text("Label (e.g. Proof 1)", fontSize = 12.sp) },
                            singleLine  = true,
                            modifier    = Modifier.fillMaxWidth().height(54.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (imageLabel.isNotBlank()) {
                                        viewModel.uploadImageForOrder(
                                            context.applicationContext,
                                            order.phoneNo, imageLabel, selectedImageUri!!
                                        ) { success ->
                                            android.widget.Toast.makeText(
                                                context, viewModel.message.value,
                                                android.widget.Toast.LENGTH_SHORT).show()
                                            if (success) { selectedImageUri = null; imageLabel = "" }
                                        }
                                    }
                                },
                                enabled  = imageLabel.isNotBlank() && !isUploading,
                                modifier = Modifier.height(36.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = DGreen)
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp),
                                        color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Upload", fontSize = 13.sp)
                                }
                            }
                            TextButton(
                                onClick  = { selectedImageUri = null },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Cancel", fontSize = 13.sp, color = DSubText)
                            }
                        }
                    }
                }
            }

            // Uploaded thumbnails (selectable)
            if (uploadedImages.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uploadedImages) { img ->
                        val isSelected = selectedImages.contains(img.id)
                        
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
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    // Visual indication of selection
                                    .background(
                                        if (isSelected) Color(0xFFC8E6C9) else Color.Transparent, 
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) DGreen else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(if (isSelected) 3.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = request,
                                    contentDescription = img.label,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DGreenBg)
                                        .combinedClickable(
                                            onClick = {
                                                // Toggle selection status
                                                selectedImages = if (isSelected) {
                                                    selectedImages - img.id
                                                } else {
                                                    selectedImages + img.id
                                                }
                                            },
                                            onLongClick = {
                                                fullScreenImageUrl = img.image
                                            }
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Selection Checkmark Icon 
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp)
                                            .background(Color(0xB32E7D32), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = {
                                        viewModel.deleteImageForOrder(order.phoneNo, img.id)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .background(Color(0x99000000), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete",
                                        tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(img.label, fontSize = 10.sp, color = if (isSelected) DGreenDark else DSubText,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 72.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Full-screen image dialog
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
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(20.dp)
                                .size(38.dp)
                                .background(Color(0x99000000), CircleShape)
                                .clickable { fullScreenImageUrl = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close",
                                tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DDivider, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // ── Courier + LLR Row ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = courierExpanded,
                    onExpandedChange = { courierExpanded = it },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value        = selectedCourier.ifEmpty { "Courier" },
                        onValueChange = {},
                        readOnly     = true,
                        modifier     = Modifier
                            .menuAnchor()
                            .weight(1f)
                            .height(50.dp),
                        textStyle    = androidx.compose.ui.text.TextStyle(
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (selectedCourier.isEmpty()) DSubText else DGreenDark
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = if (courierExpanded) Icons.Default.KeyboardArrowUp
                                              else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint     = DSubText
                            )
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = DGreen,
                            unfocusedBorderColor = DDivider,
                            focusedContainerColor   = DGreenBg,
                            unfocusedContainerColor = DGreenBg
                        )
                    )
                    ExposedDropdownMenu(
                        expanded  = courierExpanded,
                        onDismissRequest = { courierExpanded = false }
                    ) {
                        courierList.forEach { courier ->
                            DropdownMenuItem(
                                text = {
                                    Text(courier, fontSize = 13.sp,
                                        fontWeight = if (courier == selectedCourier)
                                            FontWeight.Bold else FontWeight.Normal,
                                        color = if (courier == selectedCourier) DGreen
                                                else Color(0xFF263238))
                                },
                                onClick = {
                                    viewModel.updateCourier(orderKey, courier)
                                    courierExpanded = false
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value         = llrNumber,
                    onValueChange = { viewModel.updateLlr(orderKey, it) },
                    modifier      = Modifier
                        .weight(1f)
                        .height(50.dp),
                    placeholder   = {
                        Text("LLR No", fontSize = 12.sp, color = DSubText)
                    },
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = DGreenDark
                    ),
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = DGreen,
                        unfocusedBorderColor = DDivider,
                        focusedContainerColor   = DGreenBg,
                        unfocusedContainerColor = DGreenBg
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Submit + WB Sent Row ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Submit button
                Button(
                    onClick = {
                        viewModel.updateDispatchDetails(
                            order.customerName, order.phoneNo,
                            selectedCourier, llrNumber
                        )
                    },
                    modifier = Modifier
                        .weight(0.42f)
                        .height(44.dp),
                    shape  = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DGreen)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Submit",
                        tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Submit", fontSize   = 13.sp, maxLines = 1,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }

                // WB Sent button
                Button(
                    onClick = {
                        val msg = buildString {
                            append("---------------------------------\n")
                            append("Order Details:\n\n")
                            append("Name: ${order.customerName}\n")
                            if (order.address.isNotBlank())
                                append("Address: ${order.address}\n")
                            val phone = order.phoneNo.ifBlank { order.whatsapp }
                            if (phone.isNotBlank()) append("Phone: $phone\n")
                            if (selectedCourier.isNotBlank())
                                append("Courier: $selectedCourier\n")
                            if (llrNumber.isNotBlank())
                                append("Booking LLR No: $llrNumber\n")
                            append("Status: Dispatched\n")
                            append("---------------------------------\n")
                        }
                        var waNumber = order.whatsapp.ifBlank { order.phoneNo }
                            .replace(Regex("[^0-9]"), "")
                        if (waNumber.length == 10) waNumber = "91$waNumber"

                        if (uploadedImages.isNotEmpty()) {
                            isSendingWb = true
                            
                            // Determine which images to send
                            val imagesToSend = if (selectedImages.isNotEmpty()) {
                                uploadedImages.filter { selectedImages.contains(it.id) }
                            } else {
                                uploadedImages
                            }

                            viewModel.downloadSpecificImagesForWhatsApp(context, order.phoneNo, imagesToSend) { localUrisList ->
                                val localUris = ArrayList(localUrisList)
                                isSendingWb = false

                                if (localUris.isEmpty()) {
                                    android.widget.Toast.makeText(context,
                                        "Could not download images, sending text only",
                                        android.widget.Toast.LENGTH_SHORT).show()
                                    val waUri = Uri.parse("https://wa.me/$waNumber?text=${Uri.encode(msg)}")
                                    val fallbackIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                        setPackage("com.whatsapp.w4b")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try { context.startActivity(fallbackIntent) }
                                    catch (e: Exception) {
                                        fallbackIntent.setPackage("com.whatsapp")
                                        try { context.startActivity(fallbackIntent) }
                                        catch (e2: Exception) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, waUri)
                                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                        }
                                    }
                                    return@downloadSpecificImagesForWhatsApp
                                }

                                val jidStr = "$waNumber@s.whatsapp.net"
                                val sendIntent = if (localUris.size == 1) {
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, localUris[0])
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                        putExtra("jid", jidStr)
                                        putExtra("sms_body", msg)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                } else {
                                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "image/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, localUris)
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                        putExtra("jid", jidStr)
                                        putExtra("sms_body", msg)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }
                                
                                android.widget.Toast.makeText(context,
                                    "Opening WhatsApp: Select contact for unsaved numbers.",
                                    android.widget.Toast.LENGTH_LONG).show()

                                sendIntent.setPackage("com.whatsapp.w4b")
                                try { context.startActivity(sendIntent) }
                                catch (e: Exception) {
                                    sendIntent.setPackage("com.whatsapp")
                                    try { context.startActivity(sendIntent) }
                                    catch (e2: Exception) {
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, "Send via WhatsApp")
                                        )
                                    }
                                }
                            }
                        } else {
                            val waUri = Uri.parse("https://wa.me/$waNumber?text=${Uri.encode(msg)}")
                            val textIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                                setPackage("com.whatsapp.w4b")
                            }
                            try { context.startActivity(textIntent) }
                            catch (e: Exception) {
                                textIntent.setPackage("com.whatsapp")
                                try { context.startActivity(textIntent) }
                                catch (e2: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, waUri)) }
                            }
                        }
                    },
                    enabled  = !isSendingWb,
                    modifier = Modifier
                        .weight(0.58f)
                        .height(44.dp),
                    shape    = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = DWaGreen)
                ) {
                    if (isSendingWb) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Preparing…", fontSize = 13.sp, maxLines = 1,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("📭", fontSize = 15.sp)
                        Spacer(Modifier.width(6.dp))
                        
                        // Dynamic text based on selection
                        val btnText = if (uploadedImages.isEmpty()) {
                            "WB Sent"
                        } else if (selectedImages.isNotEmpty()) {
                            "WB Sent (${selectedImages.size})"
                        } else {
                            "WB Sent (${uploadedImages.size})" // Default: sends all
                        }
                        
                        Text(
                            btnText,
                            fontSize   = 13.sp, // Slight reduction to prevent wrap
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }

            // ── Saved Courier + LLR display ──────────────────────────────
            if (order.courierName.isNotBlank() || order.llrNo.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape    = RoundedCornerShape(8.dp),
                    color    = DGreenBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null,
                            tint = DGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        if (order.courierName.isNotBlank()) {
                            Text(order.courierName, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, color = DGreenDark)
                        }
                        if (order.llrNo.isNotBlank()) {
                            if (order.courierName.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Text("•", fontSize = 14.sp, color = DGreenDark)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("LLR: ${order.llrNo}", fontSize = 14.sp, color = DGreenDark)
                        }
                    }
                }
            }
            } // Closes Expanded Body Content inner Column
        } // Closes outer Card Column
    } // Closes Card
} // Closes DispatchedOrderCard
