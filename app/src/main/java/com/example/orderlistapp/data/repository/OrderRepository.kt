package com.example.orderlistapp.data.repository

import android.util.Log
import com.example.orderlistapp.data.api.MarkDispatchedRequest
import com.example.orderlistapp.data.api.MarkFullPendingRequest
import com.example.orderlistapp.data.api.MarkPartialPendingRequest
import com.example.orderlistapp.data.api.RetrofitClient
import com.example.orderlistapp.data.model.DispatchedOrder
import com.example.orderlistapp.data.model.Order
import com.example.orderlistapp.data.model.PendingFullOrder
import com.example.orderlistapp.data.model.PendingMissingItem
import com.example.orderlistapp.data.model.SummaryItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class OrderRepository {
    private val api = RetrofitClient.apiService
    private val gson = Gson()

    // Parse the raw JSON string safely and return status + data array (if it's an object)
    private fun parseResponse(raw: String): JsonObject? {
        return try {
            @Suppress("DEPRECATION")
            val elem = JsonParser().parse(raw.trim())
            if (elem.isJsonObject) elem.asJsonObject else null
        } catch (e: Exception) {
            Log.e("OrderRepository", "JSON parse error: ${e.message}\nRaw: $raw")
            null
        }
    }

    private fun parseArrayResponse(raw: String): com.google.gson.JsonArray? {
        return try {
            @Suppress("DEPRECATION")
            val elem = JsonParser().parse(raw.trim())
            if (elem.isJsonArray) elem.asJsonArray else null
        } catch (e: Exception) {
            Log.e("OrderRepository", "JSON array parse error: ${e.message}\nRaw: $raw")
            null
        }
    }

    suspend fun getActiveOrders(): Result<List<Order>> {
        return try {
            val raw = api.getActiveOrders()
            val obj = parseResponse(raw)
            if (obj != null) {
                // It's the wrapped { status: "success", data: [...] } format
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return Result.Success(emptyList())
                    val orders = dataArray.map { gson.fromJson(it, Order::class.java) }
                    return Result.Success(orders)
                } else {
                    return Result.Error(obj.get("message")?.asString ?: "Failed to fetch active orders")
                }
            } else {
                // It might be a raw JSON array [...]
                val arr = parseArrayResponse(raw)
                if (arr != null) {
                    val orders = arr.map { gson.fromJson(it, Order::class.java) }
                    return Result.Success(orders)
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getActiveOrders error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getPendingFullOrders(): Result<List<PendingFullOrder>> {
        return try {
            val raw = api.getPendingFullOrders()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return Result.Success(emptyList())
                    val orders = dataArray.map { gson.fromJson(it, PendingFullOrder::class.java) }
                    return Result.Success(orders)
                } else {
                    return Result.Error(obj.get("message")?.asString ?: "Failed to fetch pending full orders")
                }
            } else {
                val arr = parseArrayResponse(raw)
                if (arr != null) {
                    val orders = arr.map { gson.fromJson(it, PendingFullOrder::class.java) }
                    return Result.Success(orders)
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getPendingFullOrders error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getPendingMissingItems(): Result<List<PendingMissingItem>> {
        return try {
            val raw = api.getPendingMissingItems()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return Result.Success(emptyList())
                    val items = dataArray.map { gson.fromJson(it, PendingMissingItem::class.java) }
                    return Result.Success(items)
                } else {
                    return Result.Error(obj.get("message")?.asString ?: "Failed to fetch missing items")
                }
            } else {
                val arr = parseArrayResponse(raw)
                if (arr != null) {
                    val items = arr.map { gson.fromJson(it, PendingMissingItem::class.java) }
                    return Result.Success(items)
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getPendingMissingItems error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getDispatchedOrders(): Result<List<DispatchedOrder>> {
        return try {
            val raw = api.getDispatchedOrders()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return Result.Success(emptyList())
                    val orders = dataArray.map { gson.fromJson(it, DispatchedOrder::class.java) }
                    return Result.Success(orders)
                } else {
                    return Result.Error(obj.get("message")?.asString ?: "Failed to fetch dispatched orders")
                }
            } else {
                val arr = parseArrayResponse(raw)
                if (arr != null) {
                    val orders = arr.map { gson.fromJson(it, DispatchedOrder::class.java) }
                    return Result.Success(orders)
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getDispatchedOrders error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markDispatched(
        customerName: String,
        phoneNo: String,
        dispatchedItems: String,
        pendingItems: String
    ): Result<String> {
        return try {
            val raw = api.markDispatched(
                MarkDispatchedRequest(
                    customerName = customerName,
                    phoneNo = phoneNo,
                    dispatchedItems = dispatchedItems,
                    pendingItems = pendingItems
                )
            )
            val obj = parseResponse(raw)
                ?: return Result.Error("Invalid response from server")
            val status = obj.get("status")?.asString ?: ""
            if (status == "success") {
                Result.Success(obj.get("message")?.asString ?: "Dispatched")
            } else {
                Result.Error(obj.get("message")?.asString ?: "Failed to mark dispatched")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "markDispatched error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markFullPending(customerName: String, phoneNo: String): Result<String> {
        return try {
            val raw = api.markFullPending(MarkFullPendingRequest(customerName = customerName, phoneNo = phoneNo))
            val obj = parseResponse(raw)
                ?: return Result.Error("Invalid response from server")
            val status = obj.get("status")?.asString ?: ""
            if (status == "success") {
                Result.Success(obj.get("message")?.asString ?: "Moved to Full Pending")
            } else {
                Result.Error(obj.get("message")?.asString ?: "Failed to mark full pending")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "markFullPending error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markDispatchedFromPending(
        customerName: String,
        phoneNo: String,
        dispatchedItems: String,
        pendingItems: String
    ): Result<String> {
        return try {
            val raw = api.markDispatchedFromPending(
                MarkDispatchedRequest(
                    action = "markDispatchedFromPending",
                    customerName = customerName,
                    phoneNo = phoneNo,
                    dispatchedItems = dispatchedItems,
                    pendingItems = pendingItems
                )
            )
            val obj = parseResponse(raw)
                ?: return Result.Error("Invalid response from server")
            val status = obj.get("status")?.asString ?: ""
            if (status == "success") {
                Result.Success(obj.get("message")?.asString ?: "Dispatched from Pending")
            } else {
                Result.Error(obj.get("message")?.asString ?: "Failed to dispatch from pending")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "markDispatchedFromPending error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun markPartialPending(
        orderName: String,
        missingItemName: String,
        remainingItems: String
    ): Result<String> {
        return try {
            val raw = api.markPartialPending(
                MarkPartialPendingRequest(
                    orderName = orderName,
                    missingItemName = missingItemName,
                    remainingItems = remainingItems
                )
            )
            val obj = parseResponse(raw)
                ?: return Result.Error("Invalid response from server")
            val status = obj.get("status")?.asString ?: ""
            if (status == "success") {
                Result.Success(obj.get("message")?.asString ?: "Partial pending updated")
            } else {
                Result.Error(obj.get("message")?.asString ?: "Failed to mark partial pending")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "markPartialPending error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getSummary(): Result<List<SummaryItem>> {
        return try {
            val raw = api.getSummary()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return Result.Success(emptyList())
                    val items = dataArray.map { gson.fromJson(it, SummaryItem::class.java) }
                    return Result.Success(items)
                } else {
                    return Result.Error(obj.get("message")?.asString ?: "Failed to fetch summary")
                }
            } else {
                val arr = parseArrayResponse(raw)
                if (arr != null) {
                    val items = arr.map { gson.fromJson(it, SummaryItem::class.java) }
                    return Result.Success(items)
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getSummary error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }
}
