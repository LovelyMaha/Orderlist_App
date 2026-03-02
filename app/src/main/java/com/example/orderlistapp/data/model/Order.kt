package com.example.orderlistapp.data.model

data class Order(
    val orderDate: String = "",
    val customerName: String = "",
    val address: String = "",
    val phoneNo: String = "",
    val whatsapp: String = "",
    val itemsOrdered: String = "",
    val itemsDispatched: String = "",
    val itemsPending: String = ""
) {
    fun getItemList(): List<String> {
        return itemsOrdered.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

data class PendingFullOrder(
    val orderDate: String = "",
    val customerName: String = "",
    val address: String = "",
    val phoneNo: String = "",
    val whatsapp: String = "",
    val itemsOrdered: String = "",
    val itemsDispatched: String = "",
    val itemsPending: String = ""
)

data class PendingMissingItem(
    val orderDate: String = "",
    val customerName: String = "",
    val address: String = "",
    val phoneNo: String = "",
    val whatsapp: String = "",
    val itemsOrdered: String = "",
    val itemsDispatched: String = "",
    val itemsPending: String = ""
) {
    fun getPendingItemList(): List<String> {
        return itemsPending.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

data class DispatchedOrder(
    val orderDate: String = "",
    val customerName: String = "",
    val address: String = "",
    val phoneNo: String = "",
    val whatsapp: String = "",
    val itemsOrdered: String = "",
    val itemsDispatched: String = "",
    val itemsPending: String = ""
) {
    fun getDispatchedItemList(): List<String> {
        return itemsDispatched.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

// Summary item fetched from Google Sheet
data class SummaryItem(
    val itemName: String = "",
    val totalQty: Int = 0
)

// Unified order for cross-list search results
data class UnifiedOrder(
    val source: String = "", // "Active", "Pending Full", "Missing Item", "Dispatched"
    val customerName: String = "",
    val phoneNo: String = "",
    val whatsapp: String = "",
    val address: String = "",
    val orderDate: String = "",
    val itemsOrdered: String = "",
    val itemsPending: String = "",
    val itemsDispatched: String = ""
) {
    fun getDisplayItems(): List<String> {
        // For display: prefer itemsOrdered, fall back to pending then dispatched
        val ordered = itemsOrdered.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ordered.isNotEmpty()) return ordered
        val pending = itemsPending.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (pending.isNotEmpty()) return pending
        return itemsDispatched.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
