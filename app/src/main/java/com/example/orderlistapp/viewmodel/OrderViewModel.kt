package com.example.orderlistapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.example.orderlistapp.data.model.DispatchedOrder
import com.example.orderlistapp.data.model.Order
import com.example.orderlistapp.data.model.OrderImage
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

    // ---- Courier Selection (local, in-memory) ----
    private val _courierList = MutableStateFlow(listOf(
        "Akr", "ST Courier", "A1", "MSS", "DTDC", "Pickup",
        "Direct Delivery", "VRL", "Rathimeena", "KPN", "India Post", "French Courier"
    ))
    val courierList: StateFlow<List<String>> = _courierList

    private val _courierSelections = MutableStateFlow<Map<String, String>>(emptyMap())
    val courierSelections: StateFlow<Map<String, String>> = _courierSelections

    private val _llrNumbers = MutableStateFlow<Map<String, String>>(emptyMap())
    val llrNumbers: StateFlow<Map<String, String>> = _llrNumbers

    private val _proofImages = MutableStateFlow<Map<String, String>>(emptyMap())
    val proofImages: StateFlow<Map<String, String>> = _proofImages

    // ---- PHP Image Upload States ----
    private val _orderImages = MutableStateFlow<Map<String, List<OrderImage>>>(emptyMap())
    val orderImages: StateFlow<Map<String, List<OrderImage>>> = _orderImages

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage

    fun loadImagesForOrder(phoneNo: String) {
        viewModelScope.launch {
            when (val result = repository.getOrderImages(phoneNo)) {
                is Result.Success -> {
                    val currentMap = _orderImages.value.toMutableMap()
                    currentMap[phoneNo] = result.data
                    _orderImages.value = currentMap
                }
                is Result.Error -> {
                    // Just log or ignore silently failing image fetch
                    android.util.Log.e("OrderViewModel", "Failed to load images for $phoneNo: ${result.message}")
                }
                else -> {}
            }
        }
    }

    fun uploadImageForOrder(
        context: Context,
        phoneNo: String,
        label: String,
        imageUri: Uri,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            when (val result = repository.uploadOrderImage(context, phoneNo, label, imageUri)) {
                is Result.Success -> {
                    _message.value = "✅ Image '$label' uploaded!"
                    // Reload images
                    loadImagesForOrder(phoneNo)
                    onComplete(true)
                }
                is Result.Error -> {
                    _message.value = "❌ Upload failed: ${result.message}"
                    onComplete(false)
                }
                else -> {}
            }
            _isUploadingImage.value = false
        }
    }

    fun deleteImageForOrder(phoneNo: String, imageId: String) {
        viewModelScope.launch {
            _isUploadingImage.value = true // Reuse loading state
            when (val result = repository.deleteOrderImage(imageId)) {
                is Result.Success -> {
                    _message.value = "✅ ${result.data}"
                    loadImagesForOrder(phoneNo) // Refresh images
                }
                is Result.Error -> {
                    _message.value = "❌ Delete failed: ${result.message}"
                }
                else -> {}
            }
            _isUploadingImage.value = false
        }
    }

    fun updateCourier(key: String, courier: String) {
        _courierSelections.value = _courierSelections.value.toMutableMap().apply { put(key, courier) }
    }

    fun updateLlr(key: String, llr: String) {
        _llrNumbers.value = _llrNumbers.value.toMutableMap().apply { put(key, llr) }
    }

    fun updateProofImage(key: String, uri: String) {
        _proofImages.value = _proofImages.value.toMutableMap().apply { put(key, uri) }
    }
    fun loadCouriers() {
        viewModelScope.launch {
            when (val result = repository.getCouriers()) {
                is Result.Success -> if (result.data.isNotEmpty()) _courierList.value = result.data
                is Result.Error -> {} // fallback to hardcoded list
                else -> {}
            }
        }
    }

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
        loadCouriers()
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
            val imageUrl = _orderImages.value[phoneNo]?.map { it.image }?.joinToString(", \n") ?: ""

            when (val result = repository.markDispatched(customerName, phoneNo, dispatchedItems, pendingItems, imageUrl)) {
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
            val imageUrl = _orderImages.value[phoneNo]?.map { it.image }?.joinToString(", \n") ?: ""
            when (val result = repository.markDispatchedFromPending(customerName, phoneNo, dispatchedItems, pendingItems, imageUrl)) {
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
    fun updateDispatchDetails(customerName: String, phoneNo: String, courierName: String, llrNo: String, proofImageStatus: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.updateDispatchDetails(customerName, phoneNo, courierName, llrNo, proofImageStatus)) {
                is Result.Success -> {
                    _message.value = "✅ Courier & LLR updated successfully!"
                    loadDispatchedOrders()
                }
                is Result.Error -> _message.value = "❌ ${result.message}"
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun removeProofImage(key: String) {
        _proofImages.value = _proofImages.value.toMutableMap().apply { remove(key) }
    }

    fun uploadProofImage(orderKey: String, uri: String, customerName: String, phoneNo: String, courierName: String, llrNo: String) {
        // Update local state immediately
        updateProofImage(orderKey, uri)
        // Sync "Images upload" status to sheet
        viewModelScope.launch {
            when (val result = repository.updateDispatchDetails(customerName, phoneNo, courierName, llrNo, "Images upload")) {
                is Result.Success -> {
                    _message.value = "✅ Image uploaded successfully!"
                    loadDispatchedOrders()
                }
                is Result.Error -> _message.value = "❌ Image sync failed: ${result.message}"
                else -> {}
            }
        }
    }

    fun clearProofImage(orderKey: String, customerName: String, phoneNo: String, courierName: String, llrNo: String) {
        // Clear local state immediately
        removeProofImage(orderKey)
        // Sync "Not Images upload" status to sheet
        viewModelScope.launch {
            when (val result = repository.updateDispatchDetails(customerName, phoneNo, courierName, llrNo, "Not Images upload")) {
                is Result.Success -> {
                    _message.value = "✅ Image removed!"
                    loadDispatchedOrders()
                }
                is Result.Error -> _message.value = "❌ Image clear failed: ${result.message}"
                else -> {}
            }
        }
    }
}
