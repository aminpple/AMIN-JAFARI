package com.example.data.repository

import com.example.data.local.*
import com.example.data.remote.CustomDesignPayload
import com.example.data.remote.NabloosApiService
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NabloosRepository(
    private val chandelierDao: ChandelierDao,
    private val articleDao: ArticleDao,
    private val customDesignRequestDao: CustomDesignRequestDao,
    private val apiService: NabloosApiService = RetrofitClient.apiService
) {
    val cachedChandeliers: Flow<List<ChandelierEntity>> = chandelierDao.getAllCached()
    val cachedArticles: Flow<List<ArticleEntity>> = articleDao.getAllCached()
    val customDesignRequests: Flow<List<CustomDesignRequestEntity>> = customDesignRequestDao.getAllRequests()

    suspend fun saveChandelier(entity: ChandelierEntity) {
        withContext(Dispatchers.IO) {
            chandelierDao.insert(entity)
        }
    }

    suspend fun saveArticle(entity: ArticleEntity) {
        withContext(Dispatchers.IO) {
            articleDao.insert(entity)
        }
    }

    suspend fun submitDesignRequest(entity: CustomDesignRequestEntity): Boolean {
        return withContext(Dispatchers.IO) {
            customDesignRequestDao.insertRequest(entity)
            try {
                val payload = CustomDesignPayload(
                    name = entity.name,
                    phone = entity.phone,
                    dimensions = entity.dimensions,
                    crystalType = entity.crystalType,
                    description = entity.description
                )
                // Try sending to remote
                val response = apiService.submitCustomDesignRequest(payload)
                if (response.success) {
                    customDesignRequestDao.insertRequest(entity.copy(isSubmitted = true))
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                // Return false silently so it stays marked as offline/not submitted
                false
            }
        }
    }

    suspend fun deleteDesignRequest(id: Int) {
        withContext(Dispatchers.IO) {
            customDesignRequestDao.deleteRequestById(id)
        }
    }

    // Fetches live products from WordPress/WooCommerce and caches them locally
    suspend fun refreshProducts(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch from WooCommerce API (uses dummy blank keys for portfolio/generic endpoints)
                val products = apiService.getProducts()
                products.forEach { prod ->
                    val imgUrl = prod.images?.firstOrNull()?.src ?: ""
                    chandelierDao.insert(
                        ChandelierEntity(
                            id = prod.id,
                            title = prod.name,
                            category = prod.categories?.firstOrNull()?.name ?: "لوستر آویز",
                            price = prod.price.ifEmpty { "تماس بگیرید" },
                            imageUrl = imgUrl,
                            description = prod.description ?: ""
                        )
                    )
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // Fetches articles/magazine posts and caches them locally
    suspend fun refreshArticles(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val posts = apiService.getPosts()
                posts.forEach { post ->
                    articleDao.insert(
                        ArticleEntity(
                            id = post.id,
                            title = post.title.rendered,
                            image = post.jetpack_featured_media_url ?: "",
                            excerpt = post.excerpt.rendered,
                            content = post.content.rendered,
                            date = post.date,
                            readingTime = "۵ دقیقه" // default estimate
                        )
                    )
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
