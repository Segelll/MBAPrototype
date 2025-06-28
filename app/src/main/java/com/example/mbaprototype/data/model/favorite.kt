// app/src/main/java/com/example/mbaprototype/data/model/Favorite.kt
package com.example.mbaprototype.data.model

import com.google.gson.annotations.SerializedName

data class FavoriteItem(
    @SerializedName("product_no") val productNo: Int
)

data class FavoriteActionResponse(
    val message: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("product_no") val productNo: Int
)