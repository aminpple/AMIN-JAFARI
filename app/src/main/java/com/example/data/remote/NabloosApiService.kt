package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.pow

// Data Models
data class ProductDto(
    val id: String,
    val name: String,
    val description: String?,
    val price: String,
    val images: List<ProductImageDto>?,
    val categories: List<CategoryDto>?
)

data class ProductImageDto(
    val src: String
)

data class CategoryDto(
    val name: String
)

data class PostDto(
    val id: String,
    val title: PostTitleDto,
    val excerpt: PostExcerptDto,
    val content: PostContentDto,
    val date: String,
    val jetpack_featured_media_url: String?
)

data class PostTitleDto(val rendered: String)
data class PostExcerptDto(val rendered: String)
data class PostContentDto(val rendered: String)

interface NabloosApiService {
    @GET("wp-json/wc/v3/products")
    suspend fun getProducts(
        @Query("per_page") perPage: Int = 20,
        @Query("consumer_key") consumerKey: String = "",
        @Query("consumer_secret") consumerSecret: String = ""
    ): List<ProductDto>

    @GET("wp-json/wp/v2/posts")
    suspend fun getPosts(
        @Query("per_page") perPage: Int = 10
    ): List<PostDto>

    @POST("wp-json/nabloos/v1/custom-design")
    suspend fun submitCustomDesignRequest(
        @Body request: CustomDesignPayload
    ): ApiResponse
}

data class CustomDesignPayload(
    val name: String,
    val phone: String,
    val dimensions: String,
    val crystalType: String,
    val description: String
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)

// OkHttp Resilient Client Configurator
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // Inject headers including GZIP acceptance
        request = request.newBuilder()
            .header("Accept-Encoding", "gzip")
            .header("User-Agent", "NabloosAndroidApp/1.0")
            .build()
        
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0
        
        while (tryCount <= maxRetries) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: IOException) {
                exception = e
            }
            
            tryCount++
            if (tryCount <= maxRetries) {
                val delay = initialDelayMs * 2.0.pow(tryCount).toLong()
                try {
                    Thread.sleep(delay)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw exception ?: IOException("Interrupted during backoff network retry")
                }
            }
        }
        
        return response ?: throw exception ?: IOException("Network timeout after $maxRetries retries")
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://nabloostr.com/"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        // 5-second connection timeout (enforced section 4 in guidelines)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor())
        .build()

    val apiService: NabloosApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(NabloosApiService::class.java)
    }
}
