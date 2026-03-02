package com.example.orderlistapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.orderlistapp.data.model.DispatchedOrder
import com.example.orderlistapp.data.model.Order
import com.example.orderlistapp.data.model.PendingFullOrder
import com.example.orderlistapp.data.model.PendingMissingItem
import com.example.orderlistapp.data.model.SummaryItem
import com.example.orderlistapp.data.model.UnifiedOrder
import com.example.orderlistapp.data.repository.OrderRepository
import com.example.orderlistapp.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()

    // ---- Active Orders ----
    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())
    val activeOrders: StateFlow<List<Order>> = _activeOrders

    // ---- Summary from Google Sheet ----
    private val _summaryItems = MutableStateFlow<List<SummaryItem>>(emptyList())
    val summaryItems: StateFlow<List<SummaryItem>> = _summaryItems

    // ---- Summary Loading ----
    private val _isSummaryLoading = MutableStateFlow(false)
    val isSummaryLoading: StateFlow<Boolean> = _isSummaryLoading

    // ---- Pending Full Orders ----
    private val _pendingFullOrders = MutableStateFlow<List<PendingFullOrder>>(emptyList())
    val pendingFullOrders: StateFlow<List<PendingFullOrder>> = _pendingFullOrders

    // ---- Pending Missing Items ----
    private val _pendingMissingItems = MutableStateFlow<List<PendingMissingItem>>(emptyList())
    val pendingMissingItems: StateFlow<List<PendingMissingItem>> = _pendingMissingItems

    // ---- Dispatched Orders ----
    private val _dispatchedOrders = MutableStateFlow<List<DispatchedOrder>>(emptyList())
    val dispatchedOrders: StateFlow<List<DispatchedOrder>> = _dispatchedOrders

    // ---- Loading ----
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ---- Error/Success Message ----
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // ---- Search Query ----
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    // ---- Unified Search Results (cross-list) ----
    val searchResults: StateFlow<List<UnifiedOrder>> = combine(
        _searchQuery,
        _activeOrders,
        _pendingFullOrders,
        _pendingMissingItems,
        _dispatchedOrders
    ) { query, active, pendingFull, pendingMissing, dispatched ->
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            val results = mutableListOf<UnifiedOrder>()

            // Active orders
            active.forEach { o ->
                if (matches(o.customerName, o.phoneNo, o.whatsapp, o.itemsOrdered, q)) {
                    results += UnifiedOrder(
                        source = "Active",
                        customerName = o.customerName,
                        phoneNo = o.phoneNo,
                        whatsapp = o.whatsapp,
                        address = o.address,
                        orderDate = o.orderDate,
                        itemsOrdered = o.itemsOrdered
                    )
                }
            }

            // Pending full orders
            pendingFull.forEach { o ->
                if (matches(o.customerName, o.phoneNo, o.whatsapp, o.itemsPending.ifBlank { o.itemsOrdered }, q)) {
                    results += UnifiedOrder(
                        source = "Pending",
                        customerName = o.customerName,
                        phoneNo = o.phoneNo,
                        whatsapp = o.whatsapp,
                        address = o.address,
                        orderDate = o.orderDate,
                        itemsOrdered = o.itemsOrdered,
                        itemsPending = o.itemsPending
                    )
                }
            }

            // Pending missing items
            pendingMissing.forEach { o ->
                if (matches(o.customerName, o.phoneNo, o.whatsapp, o.itemsPending, q)) {
                    results += UnifiedOrder(
                        source = "Missing",
                        customerName = o.customerName,
                        phoneNo = o.phoneNo,
                        whatsapp = o.whatsapp,
                        address = o.address,
                        orderDate = o.orderDate,
                        itemsOrdered = o.itemsOrdered,
                        itemsPending = o.itemsPending
                    )
                }
            }

            // Dispatched orders
            dispatched.forEach { o ->
                if (matches(o.customerName, o.phoneNo, o.whatsapp, o.itemsDispatched, q)) {
                    results += UnifiedOrder(
                        source = "Dispatched",
                        customerName = o.customerName,
                        phoneNo = o.phoneNo,
                        whatsapp = o.whatsapp,
                        address = o.address,
                        orderDate = o.orderDate,
                        itemsDispatched = o.itemsDispatched
                    )
                }
            }
            results
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun matches(name: String, phone: String, wa: String, items: String, q: String): Boolean {
        return name.lowercase().contains(q) ||
               phone.lowercase().contains(q) ||
               wa.lowercase().contains(q) ||
               items.lowercase().contains(q)
    }

    fun clearMessage() { _message.value = null }

    fun loadAllData() {
        loadActiveOrders()
        loadPendingFullOrders()
        loadPendingMissingItems()
        loadDispatchedOrders()
        loadSummary()
    }

    fun loadActiveOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getActiveOrders()) {
                is Result.Success -> {
                    _activeOrders.value = result.data
                    computeSummary(result.data)
                }
                is Result.Error -> _message.value = result.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    // Computes summary locally from the active orders — no extra API call needed
    private fun computeSummary(orders: List<Order>) {
        val rawMap = mutableMapOf<String, Int>()
        orders.forEach { order ->
            order.getItemList().forEach { itemString ->
                // Extract name = everything before the trailing number
                // e.g. "SuperNapier 200" -> name="SuperNapier", qty=200
                val trimmed = itemString.trim()
                val quantityMatch = Regex("(\\d+)\\s*$").find(trimmed)
                val quantity = quantityMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val name = if (quantityMatch != null)
                    trimmed.substring(0, quantityMatch.range.first).trim()
                else trimmed
                if (name.isNotEmpty()) {
                    rawMap[name] = rawMap.getOrDefault(name, 0) + quantity
                }
            }
        }
        _summaryItems.value = rawMap
            .map { SummaryItem(itemName = it.key, totalQty = it.value) }
            .sortedBy { it.itemName }
    }

    // Refresh summary by reloading active orders
    fun loadSummary() {
        _isSummaryLoading.value = true
        viewModelScope.launch {
            when (val result = repository.getActiveOrders()) {
                is Result.Success -> {
                    _activeOrders.value = result.data
                    computeSummary(result.data)
                }
                is Result.Error -> _message.value = result.message
                else -> {}
            }
            _isSummaryLoading.value = false
        }
    }

    fun loadPendingFullOrders() {
        viewModelScope.launch {
            when (val result = repository.getPendingFullOrders()) {
                is Result.Success -> _pendingFullOrders.value = result.data
                is Result.Error -> _message.value = result.message
                else -> {}
            }
        }
    }

    fun loadPendingMissingItems() {
        viewModelScope.launch {
            when (val result = repository.getPendingMissingItems()) {
                is Result.Success -> _pendingMissingItems.value = result.data
                is Result.Error -> _message.value = result.message
                else -> {}
            }
        }
    }

    fun loadDispatchedOrders() {
        viewModelScope.launch {
            when (val result = repository.getDispatchedOrders()) {
                is Result.Success -> _dispatchedOrders.value = result.data
                is Result.Error -> _message.value = result.message
                else -> {}
            }
        }
    }

    fun markDispatched(
        customerName: String,
        phoneNo: String,
        dispatchedItems: String,
        pendingItems: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.markDispatched(customerName, phoneNo, dispatchedItems, pendingItems)) {
                is Result.Success -> {
                    val message = if (pendingItems.isNotBlank()) {
                        "⚠️ Partially Dispatched! Missing items moved to Pending."
                    } else {
                        "✅ Order fully dispatched successfully!"
                    }
                    _message.value = message
                    loadAllData()
                }
                is Result.Error -> _message.value = "❌ ${result.message}"
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun markFullPending(customerName: String, phoneNo: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.markFullPending(customerName, phoneNo)) {
                is Result.Success -> {
                    _message.value = "⚠️ Full order moved to Pending!"
                    loadAllData()
                }
                is Result.Error -> _message.value = "❌ ${result.message}"
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun markDispatchedFromPending(
        customerName: String,
        phoneNo: String,
        dispatchedItems: String,
        pendingItems: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.markDispatchedFromPending(customerName, phoneNo, dispatchedItems, pendingItems)) {
                is Result.Success -> {
                    _message.value = "✅ Dispatched from Pending successfully!"
                    loadAllData()
                }
                is Result.Error -> _message.value = "❌ ${result.message}"
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun markPartialPending(orderName: String, missingItemName: String, remainingItems: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.markPartialPending(orderName, missingItemName, remainingItems)) {
                is Result.Success -> {
                    _message.value = "⚠️ Missing item '${missingItemName}' moved to Pending!"
                    loadAllData()
                }
                is Result.Error -> _message.value = "❌ ${result.message}"
                else -> {}
            }
            _isLoading.value = false
        }
    }
}
