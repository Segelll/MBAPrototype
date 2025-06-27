package com.example.mbaprototype.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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

// API servis arayüzü
interface ApiService {
    @POST("interactions/")
    suspend fun postInteraction(@Body interaction: InteractionRequest): Response<InteractionResponse>
}