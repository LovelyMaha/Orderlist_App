package com.example.orderlistapp.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.orderlistapp.data.local.AppDatabase
import com.example.orderlistapp.data.repository.OrderRepository
import com.example.orderlistapp.data.repository.Result

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("SYNC_TYPE") ?: return Result.failure()
        
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = OrderRepository(db.orderDao())

        return try {
            when (type) {
                "DISPATCH" -> {
                    val customerName = inputData.getString("CUSTOMER_NAME") ?: ""
                    val phoneNo = inputData.getString("PHONE_NO") ?: ""
                    val dispatchedItems = inputData.getString("DISPATCHED_ITEMS") ?: ""
                    val pendingItems = inputData.getString("PENDING_ITEMS") ?: ""
                    val imageUrl = inputData.getString("IMAGE_URL") ?: ""

                    // 1. Send WhatsApp only if not already sent
                    val currentOrder = db.orderDao().getAllActiveSync().firstOrNull { it.phoneNo == phoneNo }
                    val alreadySent = currentOrder?.messageStatus == "SENT"
                    
                    val apiSuccess = if (alreadySent) {
                        true 
                    } else {
                        repository.sendWhatsAppDispatchApi(phoneNo, customerName)
                    }
                    
                    val msgStatus = if (apiSuccess) "SENT" else "FAILED"
                    
                    // Update room status
                    db.orderDao().markOrderAsDispatched(phoneNo, msgStatus, System.currentTimeMillis())

                    // 2. Mark dispatched on server
                    val result = repository.markDispatched(
                        customerName, phoneNo, dispatchedItems, pendingItems, imageUrl
                    )
                    
                    if (result is com.example.orderlistapp.data.repository.Result.Success) {
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
                "PENDING" -> {
                    val customerName = inputData.getString("CUSTOMER_NAME") ?: ""
                    val phoneNo = inputData.getString("PHONE_NO") ?: ""

                    val result = repository.markFullPending(customerName, phoneNo)
                    if (result is com.example.orderlistapp.data.repository.Result.Success) {
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
                "DISPATCH_FROM_PENDING" -> {
                    val customerName = inputData.getString("CUSTOMER_NAME") ?: ""
                    val phoneNo = inputData.getString("PHONE_NO") ?: ""
                    val dispatchedItems = inputData.getString("DISPATCHED_ITEMS") ?: ""
                    val pendingItems = inputData.getString("PENDING_ITEMS") ?: ""
                    val imageUrl = inputData.getString("IMAGE_URL") ?: ""

                    val result = repository.markDispatchedFromPending(
                        customerName, phoneNo, dispatchedItems, pendingItems, imageUrl
                    )
                    if (result is com.example.orderlistapp.data.repository.Result.Success) {
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
                else -> Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Error syncing data: ${e.message}")
            Result.retry()
        }
    }
}
