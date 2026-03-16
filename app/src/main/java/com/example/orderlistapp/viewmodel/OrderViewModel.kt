package com.example.orderlistapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.example.orderlistapp.data.local.AppDatabase
import com.example.orderlistapp.data.local.OrderDao
import com.example.orderlistapp.data.local.OrderEntity
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
import kotlinx.coroutines.Dispatchers
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.orderlistapp.data.worker.SyncWorker

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val orderDao = database.orderDao()
    private val repository = OrderRepository(orderDao)

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

    // ---- Local Dispatch Tracker ----
    private val _dispatchStatusMap = MutableStateFlow<Map<String, Pair<String, Long>>>(emptyMap())
    val dispatchStatusMap: StateFlow<Map<String, Pair<String, Long>>> = _dispatchStatusMap

    init {
        // Bind ActiveOrders UI strictly to Room Database Flow
        viewModelScope.launch {
            orderDao.getAllActiveOrders().collect { entities ->
                val list = entities.map {
                    Order(
                        phoneNo = it.phoneNo,
                        orderDate = it.orderDate,
                        customerName = it.customerName,
                        address = it.address,
                        whatsapp = it.whatsapp,
                        itemsOrdered = it.itemsOrdered
                    )
                }
                _activeOrders.value = list
                computeSummary(list)
            }
        }
        
        // Bind Dispatch statuses to track indicators (Green for SENT, Red for FAILED)
        viewModelScope.launch {
            orderDao.getDispatchedOrders().collect { entities ->
                val map = entities.associate { it.phoneNo to Pair(it.messageStatus, it.dispatchTime) }
                _dispatchStatusMap.value = map
            }
        }
    }
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

    fun loadAllData(force: Boolean = false) {
        syncActiveOrders(force)
        loadPendingFullOrders()
        loadPendingMissingItems()
        loadDispatchedOrders()
        loadCouriers()
    }
    
    fun syncActiveOrders(force: Boolean = true) {
        viewModelScope.launch {
            // Check if we have offline data first
            val offlineCount = orderDao.getActiveOrdersCount()
            if (!force && offlineCount > 0) {
                // If not forced and we have data, skin the network call entirely for instant boot!
                return@launch
            }
            
            // Only show full-screen loading spinner if we have NO cached data
            if (_activeOrders.value.isEmpty()) {
                _isLoading.value = true
            }

            repository.syncActiveOrders()
            _isLoading.value = false
        }
    }

    fun loadActiveOrders() {
        // Explicit user refresh from the UI button should force the network
        syncActiveOrders(force = true)
    }

    // Computes summary locally from the active orders — no extra API call needed
    private fun computeSummary(orders: List<Order>) {
        viewModelScope.launch(Dispatchers.Default) {
            val rawMap = mutableMapOf<String, Int>()
            orders.forEach { order ->
                order.getItemList().forEach { itemString ->
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
                .map { com.example.orderlistapp.data.model.SummaryItem(itemName = it.key, totalQty = it.value) }
                .sortedBy { it.itemName }
        }
    }

    // Refresh summary by reloading active orders (force network hit)
    fun loadSummary() {
        _isSummaryLoading.value = true
        viewModelScope.launch {
            repository.syncActiveOrders()
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
            // 1. Instantly update UI (Optimistic Update)
            val time = System.currentTimeMillis()
            orderDao.markOrderAsDispatched(phoneNo, "QUEUED", time)
            
            val message = if (pendingItems.isNotBlank()) {
                "⚠️ Partially Dispatched! Processing in background..."
            } else {
                "✅ Order dispatched! Processing in background..."
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    getApplication<Application>(), 
                    message, 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            // 2. Queue the heavy API calls to WorkManager silently
            val imageUrl = _orderImages.value[phoneNo]?.map { it.image }?.joinToString(", \n") ?: ""
            val inputData = Data.Builder()
                .putString("SYNC_TYPE", "DISPATCH")
                .putString("CUSTOMER_NAME", customerName)
                .putString("PHONE_NO", phoneNo)
                .putString("DISPATCHED_ITEMS", dispatchedItems)
                .putString("PENDING_ITEMS", pendingItems)
                .putString("IMAGE_URL", imageUrl)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(inputData)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(getApplication()).enqueue(syncRequest)
        }
    }

    fun markFullPending(customerName: String, phoneNo: String) {
        viewModelScope.launch {
            // Optimistic deletion from Active Screen
            orderDao.markOrderAsDispatched(phoneNo, "PENDING", System.currentTimeMillis())
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    getApplication<Application>(), 
                    "⚠️ Moving to Full Pending in background...", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            val inputData = Data.Builder()
                .putString("SYNC_TYPE", "PENDING")
                .putString("CUSTOMER_NAME", customerName)
                .putString("PHONE_NO", phoneNo)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(inputData)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(getApplication()).enqueue(syncRequest)
        }
    }

    fun markDispatchedFromPending(
        customerName: String,
        phoneNo: String,
        dispatchedItems: String,
        pendingItems: String
    ) {
        viewModelScope.launch {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    getApplication<Application>(), 
                    "✅ Dispatching from Pending in background...", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            val imageUrl = _orderImages.value[phoneNo]?.map { it.image }?.joinToString(", \n") ?: ""

            val inputData = Data.Builder()
                .putString("SYNC_TYPE", "DISPATCH_FROM_PENDING")
                .putString("CUSTOMER_NAME", customerName)
                .putString("PHONE_NO", phoneNo)
                .putString("DISPATCHED_ITEMS", dispatchedItems)
                .putString("PENDING_ITEMS", pendingItems)
                .putString("IMAGE_URL", imageUrl)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(inputData)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(getApplication()).enqueue(syncRequest)
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
            when (val result = repository.updateDispatchDetails(customerName, phoneNo, courierName, llrNo, proofImageStatus)) {
                is Result.Success -> {
                    _message.value = "✅ Courier & LLR updated successfully!"
                    loadDispatchedOrders()
                }
                is Result.Error -> _message.value = "❌ ${result.message}"
                else -> {}
            }
        }
    }

    // ---- Delete Dispatched Orders ----
    private val _isDeletingDispatched = MutableStateFlow(false)
    val isDeletingDispatched: StateFlow<Boolean> = _isDeletingDispatched

    /**
     * Removes dispatched orders via the Google Apps Script Backend.
     * filterType: "previousDay"  -> delete orders whose orderDate < today
     *             "previousWeek" -> delete orders whose orderDate < (today - 7 days)
     *             "all"          -> delete every dispatched order
     */
    fun deleteDispatchedOrders(filterType: String) {
        viewModelScope.launch {
            _isDeletingDispatched.value = true
            when (val result = repository.deleteDispatchedOrders(filterType)) {
                is Result.Success -> {
                    _message.value = "✅ ${result.data}"
                    loadDispatchedOrders()
                }
                is Result.Error -> {
                    _message.value = "❌ Delete failed: ${result.message}"
                }
                else -> {}
            }
            _isDeletingDispatched.value = false
        }
    }

    // Set of phone numbers whose images have already been loaded this session
    private val _imageLoadedSet = mutableSetOf<String>()

    fun loadImagesForOrder(phoneNo: String, forceReload: Boolean = false) {
        if (!forceReload && _imageLoadedSet.contains(phoneNo)) return // already fetched
        viewModelScope.launch {
            when (val result = repository.getOrderImages(phoneNo)) {
                is Result.Success -> {
                    val currentMap = _orderImages.value.toMutableMap()
                    currentMap[phoneNo] = result.data
                    _orderImages.value = currentMap
                    _imageLoadedSet.add(phoneNo)
                }
                is Result.Error -> {
                    android.util.Log.e("OrderViewModel", "Failed to load images for $phoneNo: ${result.message}")
                }
                else -> {}
            }
        }
    }

    /**
     * Downloads specific (selected) images for WhatsApp sharing.
     */
    fun downloadSpecificImagesForWhatsApp(context: Context, phoneNo: String, imagesToDownload: List<OrderImage>, onComplete: (List<Uri>) -> Unit) {
        viewModelScope.launch {
            if (imagesToDownload.isEmpty()) {
                onComplete(emptyList())
                return@launch
            }
            val uris = repository.downloadImagesToCache(context.applicationContext, imagesToDownload, phoneNo)
            onComplete(uris)
        }
    }
}
