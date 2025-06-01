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
        get() = 0.0 // Prices are not displayed, so total cost is effectively zero in this context.
}