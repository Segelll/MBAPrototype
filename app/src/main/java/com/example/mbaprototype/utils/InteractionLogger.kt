package com.example.mbaprototype.utils

import android.util.Log
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.network.BulkInteractionRequest
import com.example.mbaprototype.data.network.InteractionRequest
import com.example.mbaprototype.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InteractionLogger {

    private const val TAG = "InteractionLogger"

    fun logInteraction(product: Product, interactionType: String) {
        // Geçerli olmayan 'unfavorite' gibi etkileşimleri göndermeyi engelle
        val validInteractionTypes = listOf("favorites", "bought", "scan", "click")
        if (!validInteractionTypes.contains(interactionType)) {
            Log.w(TAG, "Geçersiz etkileşim türü gönderilmedi: $interactionType")
            return
        }

        // Ağ isteğini ana iş parçacığı dışında bir Coroutine içinde yap
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = InteractionRequest(
                    product_no = product.id,
                    interaction_type = interactionType
                )
                val response = RetrofitClient.instance.postInteraction(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Etkileşim başarıyla kaydedildi: ${response.body()}")
                } else {
                    Log.e(TAG, "Etkileşim kaydedilemedi: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Etkileşim kaydı sırasında istisna oluştu", e)
            }
        }
    }

    /**
     * Logs a list of products as a 'bought' interaction in bulk.
     */
    fun logBulkBoughtInteraction(products: List<Product>) {
        if (products.isEmpty()) {
            Log.w(TAG, "Product list for bulk logging is empty. Request not sent.")
            return
        }

        val productIds = products.mapNotNull { it.id.toIntOrNull() }
        if (productIds.isEmpty()) {
            Log.w(TAG, "No valid product IDs to log in bulk.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = BulkInteractionRequest(product_nos = productIds)
                val response = RetrofitClient.instance.addBulkInteractions(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Bulk interaction logged successfully: ${response.body()?.message}")
                } else {
                    Log.e(TAG, "Failed to log bulk interaction: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during bulk interaction logging", e)
            }
        }
    }
}