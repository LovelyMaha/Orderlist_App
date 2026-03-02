package com.example.orderlistapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orderlistapp.viewmodel.OrderViewModel

// ── Design Tokens (shared with other screens) ───────────────────────────────
private val SNavy      = Color(0xFF1A237E)
private val SNavyLight = Color(0xFF3949AB)
private val SNavyBg    = Color(0xFFE8EAF6)
private val SSubText   = Color(0xFF546E7A)
private val SDivider   = Color(0xFFECEFF1)
private val SCardBg    = Color(0xFFFFFFFF)

@Composable
fun SummaryScreen(viewModel: OrderViewModel) {
    val summaryItems by viewModel.summaryItems.collectAsState()
    val isLoading    by viewModel.isSummaryLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSummary() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FB))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
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
                        .background(SNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Order Summary",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = SNavy
                    )
                    if (!isLoading && summaryItems.isNotEmpty()) {
                        Text(
                            "${summaryItems.size} item types",
                            fontSize = 12.sp,
                            color = SSubText
                        )
                    }
                }
            }
            IconButton(
                onClick = { viewModel.loadSummary() },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SNavyBg)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                    tint = SNavy, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SNavy)
            }
            summaryItems.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 52.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No summary data",
                        color = SSubText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            else -> {
                // Total row at top
                val totalQty = summaryItems.sumOf { it.totalQty }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total Plants",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            "$totalQty",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Item list
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(summaryItems) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SCardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Index badge
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SNavyBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SNavy
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                // Item name
                                Text(
                                    text = item.itemName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF263238),
                                    modifier = Modifier.weight(1f)
                                )
                                // Quantity chip
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SNavyBg
                                ) {
                                    Text(
                                        text = "${item.totalQty}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SNavyLight,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
