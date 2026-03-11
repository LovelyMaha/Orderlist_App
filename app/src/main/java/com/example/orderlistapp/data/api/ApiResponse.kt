package com.example.orderlistapp.data.api

import com.example.orderlistapp.data.model.DispatchedOrder
import com.example.orderlistapp.data.model.Order
import com.example.orderlistapp.data.model.OrderImage
import com.example.orderlistapp.data.model.PendingFullOrder
import com.example.orderlistapp.data.model.PendingMissingItem
import com.example.orderlistapp.data.model.SummaryItem
import com.google.gson.annotations.SerializedName

data class ActiveOrdersResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<Order>? = null,
    @SerializedName("message") val message: String? = null
)

data class PendingFullOrdersResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<PendingFullOrder>? = null,
    @SerializedName("message") val message: String? = null
)

data class PendingMissingItemsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<PendingMissingItem>? = null,
    @SerializedName("message") val message: String? = null
)

data class DispatchedOrdersResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<DispatchedOrder>? = null,
    @SerializedName("message") val message: String? = null
)

data class GenericResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null
)

data class SummaryResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<SummaryItem>? = null,
    @SerializedName("message") val message: String? = null
)

// PHP Image API responses
data class UploadImageResponse(
    @SerializedName("status")  val status:  String,
    @SerializedName("image")   val image:   String? = null,
    @SerializedName("label")   val label:   String? = null,
    @SerializedName("message") val message: String? = null
)

data class GetImagesResponse(
    @SerializedName("status") val status: String,
    @SerializedName("images") val images: List<OrderImage>? = null,
    @SerializedName("message") val message: String? = null
)
