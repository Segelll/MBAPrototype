package com.example.mbaprototype.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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


// API servis arayüzü
interface ApiService {
    @POST("interactions/")
    suspend fun postInteraction(@Body interaction: InteractionRequest): Response<InteractionResponse>

    /**
     * Sepete yeni bir ürün ekler.
     */
    @POST("basket/add")
    suspend fun addToBasket(@Body request: AddToBasketRequest): Response<Unit>

    /**
     * Sepetten belirtilen ürünü siler.
     */
    @DELETE("basket/delete/{product_no}")
    suspend fun deleteFromBasket(@Path("product_no") productNo: Int): Response<Unit>

    /**
     * Kullanıcının sepetindeki tüm ürünleri listeler.
     */
    @GET("basket/items/")
    suspend fun getBasketItems(): Response<List<BasketProduct>>
}