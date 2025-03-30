package com.example.mbaprototype.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class PurchaseHistory(
    val purchaseId: String,
    val purchaseDate: Date,
    val items: List<BasketItem>
) : Parcelable {
    val totalCost: Double
        get() = items.sumOf { it.product.price * it.quantity }
}