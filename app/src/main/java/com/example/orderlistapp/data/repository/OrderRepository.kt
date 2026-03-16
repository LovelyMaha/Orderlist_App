package com.example.orderlistapp.data.repository

import com.example.orderlistapp.data.local.OrderDao
import com.example.orderlistapp.data.local.OrderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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


class OrderRepository(private val orderDao: OrderDao) {
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


    suspend fun getActiveOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val raw = api.getActiveOrders()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return@withContext Result.Success(emptyList())
                    val orders = dataArray.map { gson.fromJson(it, Order::class.java) }
                    return@withContext Result.Success(orders)
                } else {
                    return@withContext Result.Error(obj.get("message")?.asString ?: "Failed to fetch active orders")
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val arr = JsonParser().parse(raw.trim()).asJsonArray
                    val orders = arr.map { gson.fromJson(it, Order::class.java) }
                    return@withContext Result.Success(orders)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "Json parse failed for active orders: $raw")
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getActiveOrders error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getPendingFullOrders(): Result<List<PendingFullOrder>> = withContext(Dispatchers.IO) {
        try {
            val raw = api.getPendingFullOrders()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return@withContext Result.Success(emptyList())
                    val orders = dataArray.map { gson.fromJson(it, PendingFullOrder::class.java) }
                    return@withContext Result.Success(orders)
                } else {
                    return@withContext Result.Error(obj.get("message")?.asString ?: "Failed to fetch pending full orders")
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val arr = JsonParser().parse(raw.trim()).asJsonArray
                    val orders = arr.map { gson.fromJson(it, PendingFullOrder::class.java) }
                    return@withContext Result.Success(orders)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "Json parse failed for pending full orders: $raw")
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getPendingFullOrders error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getPendingMissingItems(): Result<List<PendingMissingItem>> = withContext(Dispatchers.IO) {
        try {
            val raw = api.getPendingMissingItems()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return@withContext Result.Success(emptyList())
                    val items = dataArray.map { gson.fromJson(it, PendingMissingItem::class.java) }
                    return@withContext Result.Success(items)
                } else {
                    return@withContext Result.Error(obj.get("message")?.asString ?: "Failed to fetch missing items")
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val arr = JsonParser().parse(raw.trim()).asJsonArray
                    val items = arr.map { gson.fromJson(it, PendingMissingItem::class.java) }
                    return@withContext Result.Success(items)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "Json parse failed for missing items: $raw")
                }
            }
            Result.Error("Invalid response from server")
        } catch (e: Exception) {
            Log.e("OrderRepository", "getPendingMissingItems error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getDispatchedOrders(): Result<List<DispatchedOrder>> = withContext(Dispatchers.IO) {
        try {
            val raw = api.getDispatchedOrders()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return@withContext Result.Success(emptyList())
                    val orders = dataArray.map { gson.fromJson(it, DispatchedOrder::class.java) }
                    return@withContext Result.Success(orders)
                } else {
                    return@withContext Result.Error(obj.get("message")?.asString ?: "Failed to fetch dispatched orders")
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val arr = JsonParser().parse(raw.trim()).asJsonArray
                    val orders = arr.map { gson.fromJson(it, DispatchedOrder::class.java) }
                    return@withContext Result.Success(orders)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "Json parse failed for dispatched orders: $raw")
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

    suspend fun deleteDispatchedOrders(filterType: String): Result<String> {
        return try {
            val raw = api.deleteDispatchedOrders(filterType = filterType)
            val obj = parseResponse(raw)
                ?: return Result.Error("Invalid response from server")
            val status = obj.get("status")?.asString ?: ""
            if (status == "success") {
                Result.Success(obj.get("message")?.asString ?: "Deleted dispatched orders")
            } else {
                Result.Error(obj.get("message")?.asString ?: "Failed to delete dispatched orders")
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "deleteDispatchedOrders error: ${e.message}")
            Result.Error(e.localizedMessage ?: "Network error")
        }
    }


    suspend fun getCouriers(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val raw = api.getCouriers()
            val obj = parseResponse(raw)
            if (obj != null) {
                val status = obj.get("status")?.asString ?: ""
                if (status == "success") {
                    val dataArray = obj.getAsJsonArray("data") ?: return@withContext Result.Success(emptyList())
                    val couriers = dataArray.map { it.asString }
                    return@withContext Result.Success(couriers)
                } else {
                    return@withContext Result.Error(obj.get("message")?.asString ?: "Failed to fetch couriers")
                }
            } else {
                // Fallback for raw array response
                try {
                    @Suppress("DEPRECATION")
                    val arr = com.google.gson.JsonParser().parse(raw.trim()).asJsonArray
                    val couriers = arr.map { it.asString }
                    return@withContext Result.Success(couriers)
                } catch (e: Exception) {
                    Log.e("OrderRepository", "Json parse failed for couriers: $raw")
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

    // ── Download Image to Cache safely ────────────────────────────────────────
    suspend fun downloadImagesToCache(context: Context, images: List<OrderImage>, phoneNo: String): List<Uri> = withContext(Dispatchers.IO) {
        val uris = mutableListOf<Uri>()
        images.forEachIndexed { i, img ->
            try {
                val outFile = java.io.File(context.cacheDir, "wb_img_${phoneNo}_$i.jpg")
                val conn = java.net.URL(img.image).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.setRequestProperty("Accept", "image/*,*/*;q=0.8")
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.connect()
                conn.inputStream.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                conn.disconnect()
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outFile
                )
                uris.add(uri)
            } catch (e: Exception) {
                Log.e("OrderRepository", "download image error: ${e.message}")
            }
        }
        uris
    }

    suspend fun syncActiveOrders() = withContext(Dispatchers.IO) {
        val result = getActiveOrders()
        if (result is Result.Success) {
            val entities = result.data.map {
                OrderEntity(
                    phoneNo = it.phoneNo,
                    orderDate = it.orderDate,
                    customerName = it.customerName,
                    address = it.address,
                    whatsapp = it.whatsapp,
                    itemsOrdered = it.itemsOrdered
                )
            }
            orderDao.deleteAllActive()
            orderDao.insertOrders(entities)
        }
    }

    // ── WhatsApp API ────────────────────────────────────────
    suspend fun sendWhatsAppDispatchApi(toPhone: String, customerName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            // Infobip real request: send to actual customer
            val cleanPhone = toPhone.replace(Regex("[^0-9]"), "")
            val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
            val messageId = java.util.UUID.randomUUID().toString()
            
            val bodyStr = "{\"messages\":[{\"from\":\"447860088970\",\"to\":\"$formattedPhone\",\"messageId\":\"$messageId\",\"content\":{\"templateName\":\"test_whatsapp_template_en\",\"templateData\":{\"body\":{\"placeholders\":[\"$customerName\"]}},\"language\":\"en\"}}]}"
            val mediaType = "application/json".toMediaType()
            val body = bodyStr.toRequestBody(mediaType)
            
            val request = okhttp3.Request.Builder()
                .url("https://551p9g.api.infobip.com/whatsapp/1/message/template")
                .method("POST", body)
                .addHeader("Authorization", "App bb770f9639c68f98c87c9ed3112db021-434a699c-3e18-468b-9311-d55e9ee7647e")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()
                
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("OrderRepository", "WhatsApp API Response: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                // Infobip returns 200 OK even if the message fails (e.g. REJECTED_DESTINATION_NOT_REGISTERED)
                // We need to check the actual status inside the response JSON
                try {
                    val jsonResponse = org.json.JSONObject(responseBody)
                    val messagesArray = jsonResponse.optJSONArray("messages")
                    if (messagesArray != null && messagesArray.length() > 0) {
                        val firstMessage = messagesArray.getJSONObject(0)
                        val statusObj = firstMessage.optJSONObject("status")
                        val groupName = statusObj?.optString("groupName")
                        
                        if (groupName == "REJECTED") {
                            val description = statusObj.optString("description")
                            Log.e("OrderRepository", "WhatsApp message rejected: $description")
                            return@withContext false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OrderRepository", "Failed to parse WhatsApp response: ${e.message}")
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "WhatsApp API error: ${e.message}")
            false
        }
    }
}

