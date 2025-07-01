package com.example.mbaprototype.data.network

import com.example.mbaprototype.data.model.FavoriteActionResponse
import com.example.mbaprototype.data.model.FavoriteItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// İstek (request) gövdesini temsil eden veri sınıfı
data class InteractionRequest(
    val product_no: String,
    val interaction_type: String
)

// Başarılı yanıt (response) gövdesini temsil eden veri sınıfı
data class InteractionResponse(
    val message: String,
    val user_id: String,
    val product_no: Int,
    val interaction_type: String,
    val calculated_weight: Double,
    val decayed_score: Double
)

// Sepete ekleme isteği için veri sınıfı
data class AddToBasketRequest(val product_no: Int)

// Sepet listeleme yanıtındaki her bir ürünü temsil eden veri sınıfı
data class BasketProduct(val product_no: Int)

// Tavsiye yanıtını temsil eden veri sınıfı
data class RecommendationResponse(
    val recommendations: List<Int>
)

// Sepeti tamamen silme işlemi için yanıt veri sınıfı
data class DeleteAllBasketResponse(
    val message: String?,
    val detail: String?
)


// API servis arayüzü
interface ApiService {
    @POST("interactions/")
    suspend fun postInteraction(@Body interaction: InteractionRequest): Response<InteractionResponse>

    @POST("basket/add")
    suspend fun addToBasket(@Body request: AddToBasketRequest): Response<Unit>

    @DELETE("basket/delete/{product_no}")
    suspend fun deleteFromBasket(@Path("product_no") productNo: Int): Response<Unit>

    /**
     * YENİ ENDPOINT: Belirtilen kullanıcının sepetindeki tüm ürünleri siler.
     */
    @DELETE("basket/delete-all/{user_id}")
    suspend fun deleteAllFromBasket(@Path("user_id") userId: String): Response<DeleteAllBasketResponse>

    @GET("basket/items/")
    suspend fun getBasketItems(): Response<List<BasketProduct>>

    @GET("recommend/collaborative/{user_id}")
    suspend fun getCollaborativeRecommendations(
        @Path("user_id") userId: String,
        @Query("top_k") topK: Int
    ): Response<RecommendationResponse>

    @GET("recommend/basket-similarity/{user_id}")
    suspend fun getBasketSimilarityRecommendations(
        @Path("user_id") userId: String,
        @Query("top_k") topK: Int
    ): Response<RecommendationResponse>

    /**
     * Belirtilen bir ürüne benzer diğer ürünleri tavsiye eder.
     */
    @GET("recommend/content-based/{product_no}")
    suspend fun getContentBasedRecommendations(
        @Path("product_no") productNo: Int,
        @Query("top_k") topK: Int
    ): Response<RecommendationResponse>

    @GET("favorites/items/")
    suspend fun getFavoriteItems(): Response<List<FavoriteItem>>

    /**
     * Belirtilen ürünü favorilerden siler.
     */
    @DELETE("favorites/delete/{product_no}")
    suspend fun deleteFavorite(@Path("product_no") productNo: Int): Response<FavoriteActionResponse>
}