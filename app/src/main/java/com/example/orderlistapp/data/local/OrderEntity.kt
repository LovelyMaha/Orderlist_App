package com.example.orderlistapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_orders")
data class OrderEntity(
    @PrimaryKey
    val phoneNo: String,
    val orderDate: String,
    val customerName: String,
    val address: String,
    val whatsapp: String,
    val itemsOrdered: String,
    
    // Status tracking for dispatch workflow
    val messageStatus: String = "NONE", // NONE, SENT, FAILED
    val dispatchTime: Long = 0L,
    val isDispatched: Boolean = false // Custom flag to locally track dispatch if needed
)
