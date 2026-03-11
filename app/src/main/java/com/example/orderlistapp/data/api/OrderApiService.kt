package com.example.orderlistapp.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

const val BASE_URL    = "https://script.google.com/"
const val SCRIPT_PATH = "macros/s/AKfycbzs0sXJVV4W9EHgApMqgBx8m1WX2wrX41VbjSWN8J1kybwjKlWvoflvabgy_BvpUo_c/exec"
const val WEB_APP_URL = "https://script.google.com/$SCRIPT_PATH"

// ── PHP Server Constants ──────────────────────────────────────────────────────
const val PHP_BASE_URL        = "https://whitesmoke-gnu-497382.hostingersite.com/"
const val PHP_UPLOAD_PATH     = "upload_image.php"
const val PHP_GET_IMAGES_PATH = "get_images.php"

// ── Google Apps Script API ────────────────────────────────────────────────────
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

    @GET(SCRIPT_PATH)
    suspend fun getCouriers(@Query("action") action: String = "getCouriers"): String

    // All write operations use GET with query parameters (GAS doGet pattern)
    @GET(SCRIPT_PATH)
    suspend fun markDispatched(
        @Query("action") action: String = "markDispatched",
        @Query("customerName") customerName: String,
        @Query("phoneNo") phoneNo: String,
        @Query("dispatchedItems") dispatchedItems: String,
        @Query("pendingItems") pendingItems: String,
        @Query("imageUrl") imageUrl: String = ""
    ): String

    @GET(SCRIPT_PATH)
    suspend fun markDispatchedFromPending(
        @Query("action") action: String = "markDispatchedFromPending",
        @Query("customerName") customerName: String,
        @Query("phoneNo") phoneNo: String,
        @Query("dispatchedItems") dispatchedItems: String,
        @Query("pendingItems") pendingItems: String,
        @Query("imageUrl") imageUrl: String = ""
    ): String

    @GET(SCRIPT_PATH)
    suspend fun markFullPending(
        @Query("action") action: String = "markFullPending",
        @Query("customerName") customerName: String,
        @Query("phoneNo") phoneNo: String
    ): String

    @GET(SCRIPT_PATH)
    suspend fun markPartialPending(
        @Query("action") action: String = "markPartialPending",
        @Query("orderName") orderName: String,
        @Query("missingItemName") missingItemName: String,
        @Query("remainingItems") remainingItems: String
    ): String

    @GET(SCRIPT_PATH)
    suspend fun updateDispatchDetails(
        @Query("action") action: String = "updateDispatchDetails",
        @Query("customerName") customerName: String,
        @Query("phoneNo") phoneNo: String,
        @Query("courierName") courierName: String,
        @Query("llrNo") llrNo: String,
        @Query("proofImageStatus") proofImageStatus: String = "",
        @Query("imageUrl") imageUrl: String = ""
    ): String
}

// ── PHP Image API (multipart upload + get images) ─────────────────────────────
interface PhpImageApiService {
    @Multipart
    @POST(PHP_UPLOAD_PATH)
    suspend fun uploadImage(
        @Part("orderphonenumber") phone: RequestBody,
        @Part("label") label: RequestBody,
        @Part image: MultipartBody.Part
    ): UploadImageResponse

    @GET(PHP_GET_IMAGES_PATH)
    suspend fun getImages(
        @Query("phone") phone: String,
        @Query("t") timestamp: Long
    ): GetImagesResponse

    @GET("delete_image.php")
    suspend fun deleteImage(
        @Query("id") id: String
    ): BasicPhpResponse
}

data class BasicPhpResponse(
    val status: String,
    val message: String? = null
)

// ── Google Apps Script Retrofit Client ───────────────────────────────────────
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Google Apps Script redirects GET requests.
     * OkHttp follows GET redirects automatically.
     */
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val apiService: OrderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(OrderApiService::class.java)
    }
}

// ── PHP Server Retrofit Client ────────────────────────────────────────────────
object PhpRetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val imageApiService: PhpImageApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PHP_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhpImageApiService::class.java)
    }
}
