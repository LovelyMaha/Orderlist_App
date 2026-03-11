package com.example.orderlistapp.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.orderlistapp.data.api.PhpRetrofitClient
import com.example.orderlistapp.data.api.RetrofitClient
import com.example.orderlistapp.data.model.DispatchedOrder
import com.example.orderlistapp.data.model.Order
import com.example.orderlistapp.data.model.OrderImage
import com.example.orderlistapp.data.model.PendingFullOrder
import com.example.orderlistapp.data.model.PendingMissingItem
import com.example.orderlistapp.data.model.SummaryItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class OrderRepository {
    private val api     = RetrofitClient.apiService
    private val phpApi  = PhpRetrofitClient.imageApiService
    private val gson    = Gson()

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
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return Result.Success(emptyList())
                    val orders = dataArray.map { gson.fromJson(it, Order::class.java) }
                    return Result.Success(orders)
                } else {
                    return Result.Error(obj.get("message")?.asString ?: "Failed to fetch active orders")
                }
            } else {
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
        pendingItems: String,
        imageUrl: String = ""
    ): Result<String> {
        return try {
            val raw = api.markDispatched(
                customerName = customerName,
                phoneNo = phoneNo,
                dispatchedItems = dispatchedItems,
                pendingItems = pendingItems,
                imageUrl = imageUrl
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
            val raw = api.markFullPending(customerName = customerName, phoneNo = phoneNo)
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
        pendingItems: String,
        imageUrl: String = ""
    ): Result<String> {
        return try {
            val raw = api.markDispatchedFromPending(
                customerName = customerName,
                phoneNo = phoneNo,
                dispatchedItems = dispatchedItems,
                pendingItems = pendingItems,
                imageUrl = imageUrl
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
                orderName = orderName,
                missingItemName = missingItemName,
                remainingItems = remainingItems
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

    suspend fun updateDispatchDetails(
        customerName: String,
        phoneNo: String,
        courierName: String,
        llrNo: String,
        proofImageStatus: String = "",
        imageUrl: String = ""
    ): Result<String> {
        return try {
            val raw = api.updateDispatchDetails(
                customerName = customerName,
                phoneNo = phoneNo,
                courierName = courierName,
                llrNo = llrNo,
                proofImageStatus = proofImageStatus,
                imageUrl = imageUrl
            )
            val obj = parseResponse(raw)
                ?: return Result.Error("Invalid response from server")
            val status = obj.get("status")?.asString ?: ""
            if (status == "success") {
                Result.Success(obj.get("message")?.asString ?: "Updated dispatch details")
            } else {
                Result.Error(obj.get("message")?.asString ?: "Failed to update dispatch details")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "updateDispatchDetails error: ${e.message}")
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

    suspend fun getCouriers(): Result<List<String>> {
        return try {
            val raw = api.getCouriers()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return Result.Success(emptyList())
                    val couriers = dataArray.map { it.asString }
                    return Result.Success(couriers)
                } else {
                    return Result.Error(obj.get("message")?.asString ?: "Failed to fetch couriers")
                }
            } else {
                val arr = parseArrayResponse(raw)
                if (arr != null) {
                    val couriers = arr.map { it.asString }
                    return Result.Success(couriers)
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getCouriers error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    // ── PHP Image Upload ──────────────────────────────────────────────────────
    suspend fun uploadOrderImage(
        context: Context,
        phoneNo: String,
        label: String,
        imageUri: Uri
    ): Result<String> {
        return try {
            // Read and compress image
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return Result.Error("Cannot read image file")
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return Result.Error("Cannot decode image")

            // Resize if wider than 1200px
            val maxW = 1200
            val bitmap = if (originalBitmap.width > maxW) {
                val newH = (originalBitmap.height.toFloat() / originalBitmap.width * maxW).toInt()
                Bitmap.createScaledBitmap(originalBitmap, maxW, newH, true)
            } else originalBitmap

            // Compress to JPEG
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageBytes = baos.toByteArray()
            baos.close()

            // Build multipart
            val phoneBody  = phoneNo.toRequestBody("text/plain".toMediaType())
            val labelBody  = label.toRequestBody("text/plain".toMediaType())
            val imagePart  = MultipartBody.Part.createFormData(
                "image", "order_image.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )

            val response = phpApi.uploadImage(phoneBody, labelBody, imagePart)
            if (response.status == "success" && response.image != null) {
                Result.Success(response.image)
            } else {
                Result.Error(response.message ?: "Upload failed")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "uploadOrderImage error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Upload error")
        }
    }

    // ── PHP Get Images ────────────────────────────────────────────────────────
    suspend fun getOrderImages(phoneNo: String): Result<List<OrderImage>> {
        return try {
            val response = phpApi.getImages(phoneNo, System.currentTimeMillis())
            if (response.status == "success") {
                Result.Success(response.images ?: emptyList())
            } else {
                Result.Error(response.message ?: "Failed to fetch images")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "getOrderImages error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    // ── PHP Delete Image ──────────────────────────────────────────────────────
    suspend fun deleteOrderImage(id: String): Result<String> {
        return try {
            val response = phpApi.deleteImage(id)
            if (response.status == "success") {
                Result.Success(response.message ?: "Deleted successfully")
            } else {
                Result.Error(response.message ?: "Failed to delete image")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "deleteOrderImage error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }
}
