package com.example.orderlistapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM active_orders WHERE isDispatched = 0 ORDER BY orderDate DESC")
    fun getAllActiveOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT COUNT(*) FROM active_orders WHERE isDispatched = 0")
    suspend fun getActiveOrdersCount(): Int

    @Query("SELECT * FROM active_orders WHERE isDispatched = 0")
    suspend fun getAllActiveSync(): List<OrderEntity>

    @Query("SELECT * FROM active_orders WHERE isDispatched = 1 ORDER BY dispatchTime DESC")
    fun getDispatchedOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE active_orders SET messageStatus = :status, isDispatched = 1, dispatchTime = :time WHERE phoneNo = :phoneNo")
    suspend fun markOrderAsDispatched(phoneNo: String, status: String, time: Long)

    @Query("DELETE FROM active_orders WHERE isDispatched = 0")
    suspend fun deleteAllActive()
}
