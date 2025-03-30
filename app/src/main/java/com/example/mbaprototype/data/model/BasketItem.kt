package com.example.mbaprototype.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BasketItem(
    val product: Product,
    var quantity: Int = 1
) : Parcelable