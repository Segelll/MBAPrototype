package com.example.mbaprototype.data.model

import com.google.gson.annotations.SerializedName

// Bu veri sınıfı, sunucudan gelen her bir favori öğesini temsil eder.
// İsimlendirme standart hale getirildi.
data class FavoriteItem(
    @SerializedName("product_no") val productNo: Int
)

// Bu veri sınıfı, favori ekleme/silme işlemlerinden sonra sunucudan
// dönen cevabı temsil eder.
data class FavoriteActionResponse(
    val message: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("product_no") val productNo: Int
)