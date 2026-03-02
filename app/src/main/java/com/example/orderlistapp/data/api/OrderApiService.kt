package com.example.orderlistapp.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

const val BASE_URL    = "https://script.google.com/"
const val SCRIPT_PATH = "macros/s/AKfycbxaxYRXNaqId-6Ui49NbkqOCQjk5Z_p3xJqdFrNhDMTAiMIfMb6YFcpC9Og4AONGirO/exec"
const val WEB_APP_URL = "https://script.google.com/$SCRIPT_PATH"

interface OrderApiService {
    @GET(SCRIPT_PATH)
    suspend fun getActiveOrders(@Query("action") action: String = "getActiveOrders"): String

    @GET(SCRIPT_PATH)
    suspend fun getPendingFullOrders(@Query("action") action: String = "getPendingFullOrders"): String

    @GET(SCRIPT_PATH)
    suspend fun getPendingMissingItems(@Query("action") action: String = "getPendingMissingItems"): String

    @GET(SCRIPT_PATH)
    suspend fun getDispatchedOrders(@Query("action") action: String = "getDispatchedOrders"): String

    @GET(SCRIPT_PATH)
    suspend fun getSummary(@Query("action") action: String = "getSummary"): String

    @POST(SCRIPT_PATH)
    suspend fun markDispatched(@Body request: MarkDispatchedRequest): String

    @POST(SCRIPT_PATH)
    suspend fun markDispatchedFromPending(@Body request: MarkDispatchedRequest): String

    @POST(SCRIPT_PATH)
    suspend fun markFullPending(@Body request: MarkFullPendingRequest): String

    @POST(SCRIPT_PATH)
    suspend fun markPartialPending(@Body request: MarkPartialPendingRequest): String
}

data class MarkDispatchedRequest(
    val action: String = "markDispatched",
    val customerName: String,
    val phoneNo: String,
    val dispatchedItems: String,
    val pendingItems: String
)

data class MarkFullPendingRequest(
    val action: String = "markFullPending",
    val customerName: String,
    val phoneNo: String
)

data class MarkPartialPendingRequest(
    val action: String = "markPartialPending",
    val orderName: String,
    val missingItemName: String,
    val remainingItems: String
)

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Manually follow Google Apps Script 302 redirects preserving GET + query params
    private val gasRedirectInterceptor = okhttp3.Interceptor { chain ->
        var request = chain.request()
        var response = chain.proceed(request)
        var redirectCount = 0

        while ((response.code == 301 || response.code == 302 || response.code == 303)
            && redirectCount < 5
        ) {
            val location = response.header("Location") ?: break
            response.close()
            request = Request.Builder()
                .url(location)
                .get()
                .build()
            response = chain.proceed(request)
            redirectCount++
        }
        response
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(gasRedirectInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    val apiService: OrderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create()) // For parsing raw String responses
            .addConverterFactory(GsonConverterFactory.create()) // For converting @Body objects to JSON
            .build()
            .create(OrderApiService::class.java)
    }
}
